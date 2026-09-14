package com.JJIN.domain.recommendation.locality.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.JJIN.domain.place.entity.Place;
import com.JJIN.domain.place.entity.PlaceLocalizedContent;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.repository.PlaceLocalizedContentRepository;
import com.JJIN.domain.place.tourapi.config.TourApiProperties;
import com.JJIN.domain.recommendation.locality.cache.LocalityScoreCache;
import com.JJIN.domain.recommendation.locality.client.AttractionConcentrationApiClient;
import com.JJIN.domain.recommendation.locality.client.ConcentrationApiClient;
import com.JJIN.domain.recommendation.locality.dto.DistrictConcentration;
import com.JJIN.domain.recommendation.locality.exception.ConcentrationApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관광 빅데이터 지역별 방문자수 API로 시군구 baseline 로컬도를 산출한 뒤,
 * KTO 관광지 집중률 API의 tAtsNm 별 값을 per-Place modifier로 결합해 세밀도를 확보한다.
 *
 * baseline = 시군구 현지인 비율 (localRatio, 방문자수 API)
 * modifier = 1 - avg(cnctrRate)/100 (관광지 집중률 API, tAtsNm 매칭 필요)
 *
 * 관광지 집중률 API에 등록된 관광지(대개 KTO 유명 관광지)만 modifier가 붙고,
 * 나머지 로컬 스팟은 baseline 그대로. 결합 비율은 attractionConcentration.baselineWeight로 조정.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalityScoreCalculator {

	private static final double NEUTRAL_SCORE = 0.5;

	private final ConcentrationApiClient concentrationApiClient;
	private final AttractionConcentrationApiClient attractionConcentrationApiClient;
	private final LocalityScoreCache cache;
	private final PlaceLocalizedContentRepository localizedContentRepository;
	private final TourApiProperties properties;
	private final Clock clock;

	/**
	 * Place 목록의 로컬도 점수를 시군구 baseline + per-Place modifier로 산출한다.
	 */
	public Map<Long, Double> resolveScores(final List<Place> places) {
		if (places.isEmpty()) {
			return Map.of();
		}

		Map<String, DistrictConcentration> districtBaselines = loadDistrictBaselines(places);
		Map<Long, Double> perPlaceModifiers = loadPerPlaceModifiers(places);
		return combine(places, districtBaselines, perPlaceModifiers);
	}

	/**
	 * 지정 지역(regionCode 2자리) 내 시군구별 현지인 비율(localRatio)을 반환한다.
	 * 시군구 선정(로컬도 레벨에 맞는 지역 고르기)에 사용한다.
	 *
	 * @return district(3자리) → localRatio [0.0, 1.0]
	 */
	public Map<String, Double> districtLocalRatios(final String regionCode) {
		if (regionCode == null || regionCode.isBlank()) {
			return Map.of();
		}
		Map<String, DistrictConcentration> nationwide = refreshNationwide();
		if (nationwide.isEmpty()) {
			return Map.of();
		}
		nationwide.forEach((code, snap) -> cache.save(snap, properties.visitorStats().cacheTtl()));

		Map<String, Double> result = new HashMap<>();
		nationwide.forEach((signgu, snap) -> {
			if (signgu.startsWith(regionCode) && signgu.length() > regionCode.length() && !snap.isEmpty()) {
				result.put(signgu.substring(regionCode.length()), snap.localRatio());
			}
		});
		return result;
	}

	/** 단일 장소의 로컬도. 시군구 코드가 없으면 중립값(0.5)을 반환한다. */
	public double resolveScore(final Place place) {
		String districtCode = place.getLegalDongDistrictCode();
		if (districtCode == null || districtCode.isBlank()) {
			return NEUTRAL_SCORE;
		}
		return resolveScores(List.of(place)).getOrDefault(place.getId(), NEUTRAL_SCORE);
	}

	private Map<String, DistrictConcentration> loadDistrictBaselines(final List<Place> places) {
		Set<String> requiredSigngu = new HashSet<>();
		for (Place place : places) {
			String signgu = signguCodeOf(place);
			if (signgu != null) {
				requiredSigngu.add(signgu);
			}
		}

		Map<String, DistrictConcentration> snapshots = new HashMap<>();
		for (String code : requiredSigngu) {
			cache.find(code).ifPresent(snap -> snapshots.put(code, snap));
		}
		if (snapshots.keySet().containsAll(requiredSigngu)) {
			return snapshots;
		}

		Map<String, DistrictConcentration> refreshed = refreshNationwide();
		if (refreshed.isEmpty()) {
			return snapshots;
		}
		refreshed.forEach((code, snap) -> cache.save(snap, properties.visitorStats().cacheTtl()));
		snapshots.putAll(refreshed);
		return snapshots;
	}

	/** Place의 region(2) + district(3)를 방문자수 API 스냅샷 키(5자리 signguCode)로 결합한다. */
	private String signguCodeOf(final Place place) {
		String region = place.getLegalDongRegionCode();
		String district = place.getLegalDongDistrictCode();
		if (region == null || region.isBlank() || district == null || district.isBlank()) {
			return null;
		}
		return region + district;
	}

	private Map<String, DistrictConcentration> refreshNationwide() {
		int windowDays = Math.max(1, properties.visitorStats().lookbackDays());
		int maxWindows = Math.max(1, properties.visitorStats().maxLookbackWindows());
		LocalDate cursorEnd = LocalDate.now(clock).minusDays(1);

		// 방문자수 API는 데이터 반영이 수개월 지연될 수 있어, 데이터가 나올 때까지
		// 창을 과거로 밀며 재시도한다(지연 폭에 자동 적응). 키는 5자리 signguCode.
		for (int attempt = 0; attempt < maxWindows; attempt++) {
			LocalDate end = cursorEnd;
			LocalDate start = end.minusDays(windowDays - 1L);
			try {
				Map<String, DistrictConcentration> snapshot =
					concentrationApiClient.loadDistrictSnapshots(start, end);
				if (!snapshot.isEmpty()) {
					if (attempt > 0) {
						log.info("방문자수 데이터 확보: {}~{} (과거로 {}창 이동)", start, end, attempt);
					}
					return snapshot;
				}
			} catch (ConcentrationApiException exception) {
				log.warn("전국 방문자수 배치 fetch 실패: window={}~{}, msg={}", start, end, exception.getMessage());
				return Map.of();
			}
			cursorEnd = start.minusDays(1);
		}
		log.warn("방문자수 데이터를 최근 {}개 창에서 찾지 못함, baseline 미확보", maxWindows);
		return Map.of();
	}

	/**
	 * (regionCode, districtCode) 조합별로 관광지 집중률 맵을 캐시/API에서 로드한 뒤,
	 * Place의 국문 이름과 매칭해 placeId → modifier(0.0~1.0)를 반환한다.
	 * 매칭 실패는 결과 맵에서 제외해 combine 단계에서 baseline만 사용되도록 한다.
	 */
	private Map<Long, Double> loadPerPlaceModifiers(final List<Place> places) {
		Set<DistrictKey> requiredKeys = places.stream()
			.map(DistrictKey::from)
			.filter(k -> k != null)
			.collect(Collectors.toSet());
		if (requiredKeys.isEmpty()) {
			return Map.of();
		}

		Map<DistrictKey, Map<String, Double>> attractionMaps = new HashMap<>();
		for (DistrictKey key : requiredKeys) {
			attractionMaps.put(key, loadAttractionMap(key));
		}

		List<Long> placeIds = places.stream().map(Place::getId).toList();
		Map<Long, String> placeIdToName = localizedContentRepository
			.findAllByPlaceIdInAndLocaleAndVisibleTrue(placeIds, PlaceLocale.KO).stream()
			.collect(Collectors.toMap(
				content -> content.getPlace().getId(),
				PlaceLocalizedContent::getName,
				(a, b) -> a
			));

		Map<Long, Double> modifiers = new HashMap<>();
		for (Place place : places) {
			DistrictKey key = DistrictKey.from(place);
			if (key == null) {
				continue;
			}
			String name = placeIdToName.get(place.getId());
			if (name == null || name.isBlank()) {
				continue;
			}
			Double concentration = attractionMaps.getOrDefault(key, Map.of()).get(name);
			if (concentration != null) {
				modifiers.put(place.getId(), clamp01(1.0 - concentration));
			}
		}
		return modifiers;
	}

	private Map<String, Double> loadAttractionMap(final DistrictKey key) {
		// 관광지 집중률 API의 signguCd는 5자리(regionCode 2 + district 3)를 요구한다.
		String signguCd = key.regionCode() + key.districtCode();
		Optional<Map<String, Double>> cached = cache.findAttractions(key.regionCode(), signguCd);
		if (cached.isPresent()) {
			return cached.get();
		}
		try {
			Map<String, Double> fetched = attractionConcentrationApiClient
				.loadAttractionConcentrations(key.regionCode(), signguCd);
			cache.saveAttractions(
				key.regionCode(), signguCd, fetched,
				properties.attractionConcentration().cacheTtl());
			return fetched;
		} catch (ConcentrationApiException exception) {
			log.warn("관광지 집중률 조회 실패, modifier 건너뜀: region={}, signgu={}, msg={}",
				key.regionCode(), signguCd, exception.getMessage());
			return Map.of();
		}
	}

	private Map<Long, Double> combine(
		final List<Place> places,
		final Map<String, DistrictConcentration> baselines,
		final Map<Long, Double> modifiers
	) {
		double baselineWeight = clamp01(properties.attractionConcentration().baselineWeight());
		Map<Long, Double> result = new HashMap<>();
		for (Place place : places) {
			String signgu = signguCodeOf(place);
			if (signgu == null) {
				result.put(place.getId(), NEUTRAL_SCORE);
				continue;
			}
			DistrictConcentration snap = baselines.get(signgu);
			double baseline = snap == null || snap.isEmpty()
				? NEUTRAL_SCORE : clamp01(snap.localRatio());

			Double modifier = modifiers.get(place.getId());
			double score = modifier == null
				? baseline
				: clamp01(baselineWeight * baseline + (1.0 - baselineWeight) * modifier);
			result.put(place.getId(), score);
		}
		return result;
	}

	private double clamp01(final double value) {
		if (value < 0.0) {
			return 0.0;
		}
		if (value > 1.0) {
			return 1.0;
		}
		return value;
	}

	private record DistrictKey(String regionCode, String districtCode) {

		static DistrictKey from(final Place place) {
			String region = place.getLegalDongRegionCode();
			String district = place.getLegalDongDistrictCode();
			if (region == null || region.isBlank() || district == null || district.isBlank()) {
				return null;
			}
			return new DistrictKey(region, district);
		}
	}
}
