package com.JJIN.domain.recommendation.locality.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.JJIN.domain.place.entity.Place;
import com.JJIN.domain.recommendation.locality.cache.LocalityScoreCache;
import com.JJIN.domain.recommendation.locality.client.ConcentrationApiClient;
import com.JJIN.domain.place.tourapi.config.TourApiProperties;
import com.JJIN.domain.recommendation.locality.dto.DistrictConcentration;
import com.JJIN.domain.recommendation.locality.exception.ConcentrationApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관광 빅데이터 지역별 방문자수 API로 시군구별 방문자 구성을 얻어 로컬도 점수를 산출한다.
 *
 * touDivCd(1=현지인, 2=외지인, 3=외국인) 별 방문자수를 시군구 단위로 누적한 뒤
 * 로컬도 = 현지인 방문수 / 전체 방문수 로 정의한다. 값이 1.0에 가까울수록 현지인 비중이
 * 높은 로컬 장소, 0.0에 가까울수록 외지인/외국인 관광객이 몰리는 관광지 성격.
 *
 * API가 지역 필터를 받지 않아 캐시 미스 시 전국 배치 fetch로 전 시군구를 한 번에 채운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalityScoreCalculator {

	private static final double NEUTRAL_SCORE = 0.5;

	private final ConcentrationApiClient concentrationApiClient;
	private final LocalityScoreCache cache;
	private final TourApiProperties properties;
	private final Clock clock;

	/**
	 * Place 목록의 로컬도 점수를 시군구 단위로 일괄 산출한다.
	 * 하나라도 캐시 미스가 있으면 전국 스냅샷을 한 번 배치 fetch해 채운다.
	 */
	public Map<Long, Double> resolveScores(final List<Place> places) {
		if (places.isEmpty()) {
			return Map.of();
		}

		Set<String> requiredDistricts = new HashSet<>();
		for (Place place : places) {
			String code = place.getLegalDongDistrictCode();
			if (code != null && !code.isBlank()) {
				requiredDistricts.add(code);
			}
		}

		Map<String, DistrictConcentration> snapshots = loadFromCache(requiredDistricts);
		if (snapshots.keySet().containsAll(requiredDistricts)) {
			return toResult(places, snapshots);
		}

		Map<String, DistrictConcentration> refreshed = refreshNationwide();
		if (refreshed.isEmpty()) {
			return toResult(places, snapshots);
		}
		refreshed.forEach((code, snap) -> cache.save(snap, properties.visitorStats().cacheTtl()));
		snapshots.putAll(refreshed);
		return toResult(places, snapshots);
	}

	/** 단일 장소의 로컬도. 시군구 코드가 없으면 중립값(0.5)을 반환한다. */
	public double resolveScore(final Place place) {
		String districtCode = place.getLegalDongDistrictCode();
		if (districtCode == null || districtCode.isBlank()) {
			return NEUTRAL_SCORE;
		}
		return resolveScores(List.of(place)).getOrDefault(place.getId(), NEUTRAL_SCORE);
	}

	private Map<String, DistrictConcentration> loadFromCache(final Set<String> districts) {
		Map<String, DistrictConcentration> snapshots = new HashMap<>();
		for (String code : districts) {
			Optional<DistrictConcentration> cached = cache.find(code);
			cached.ifPresent(snap -> snapshots.put(code, snap));
		}
		return snapshots;
	}

	private Map<String, DistrictConcentration> refreshNationwide() {
		LocalDate today = LocalDate.now(clock);
		LocalDate end = today.minusDays(1);
		LocalDate start = end.minusDays(Math.max(1, properties.visitorStats().lookbackDays()) - 1L);

		try {
			return concentrationApiClient.loadDistrictSnapshots(start, end);
		} catch (ConcentrationApiException exception) {
			log.warn("전국 방문자수 배치 fetch 실패, 중립값 사용: msg={}", exception.getMessage());
			return Map.of();
		}
	}

	private Map<Long, Double> toResult(
		final List<Place> places,
		final Map<String, DistrictConcentration> snapshots
	) {
		Map<Long, Double> result = new HashMap<>();
		for (Place place : places) {
			String districtCode = place.getLegalDongDistrictCode();
			if (districtCode == null || districtCode.isBlank()) {
				result.put(place.getId(), NEUTRAL_SCORE);
				continue;
			}
			DistrictConcentration snap = snapshots.get(districtCode);
			double score = snap == null || snap.isEmpty()
				? NEUTRAL_SCORE : clamp01(snap.localRatio());
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
}
