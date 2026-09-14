package com.JJIN.domain.recommendation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.JJIN.domain.onboarding.entity.TravelRegion;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.Place;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.repository.PlaceLocalizedContentRepository;
import com.JJIN.domain.place.repository.PlaceRepository;
import com.JJIN.domain.recommendation.dto.TravelProfile;
import com.JJIN.domain.recommendation.locality.DistrictCentroidProvider;
import com.JJIN.domain.recommendation.locality.service.LocalityScoreCalculator;
import com.JJIN.global.geo.GeoPoint;
import com.JJIN.global.geo.GeoUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 후보 풀 수집.
 * 집중률(방문자수) API 기반으로 사용자 로컬도 레벨에 맞는 시군구를 선정하고,
 * 선정된 시군구에서 TourAPI(on-demand)로 추천 후보 장소를 조회한다.
 */
@Slf4j
@Service
public class CandidatePoolService {

	private static final int PAGE_SIZE = 50;
	private static final int MAX_PAGES = 5;
	// 선정 시군구 수 상한. 시군구가 늘수록 TourAPI on-demand 호출(시군구×콘텐츠타입×페이지)이
	// 급증해 응답 지연이 커지므로, 후보 다양성에 충분한 수로 제한한다.
	private static final int MAX_DISTRICTS = 7;

	// 코스에서 하드하게 제외할 장소 이름 키워드(KO 이름 기준 부분 일치).
	private static final List<String> BLOCKED_NAME_KEYWORDS = List.of("노브랜드", "스타벅스", "CU", "약국");

	private final LocalityScoreCalculator localityScoreCalculator;
	private final CandidateFetchWorker candidateFetchWorker;
	private final DistrictCentroidProvider districtCentroidProvider;
	private final PlaceRepository placeRepository;
	private final PlaceLocalizedContentRepository localizedContentRepository;
	private final Executor candidateFetchExecutor;

	public CandidatePoolService(
		final LocalityScoreCalculator localityScoreCalculator,
		final CandidateFetchWorker candidateFetchWorker,
		final DistrictCentroidProvider districtCentroidProvider,
		final PlaceRepository placeRepository,
		final PlaceLocalizedContentRepository localizedContentRepository,
		@Qualifier("candidateFetchExecutor") final Executor candidateFetchExecutor
	) {
		this.localityScoreCalculator = localityScoreCalculator;
		this.candidateFetchWorker = candidateFetchWorker;
		this.districtCentroidProvider = districtCentroidProvider;
		this.placeRepository = placeRepository;
		this.localizedContentRepository = localizedContentRepository;
		this.candidateFetchExecutor = candidateFetchExecutor;
	}

	/**
	 * 사용자 목표 로컬도(targetLocality)에 가장 가까운 시군구를 앵커로 선정하고,
	 * 앵커에 지리적으로 인접한 시군구를 필요 수(requiredDistrictCount)만큼 함께 선정한다.
	 * 인접 판정용 좌표는 DB에 적재된 시군구 대표 좌표를 쓰며, 좌표가 없으면 로컬도 근접 순으로 폴백한다.
	 * 집중률 데이터가 없으면 여행 지역에 지정된 시군구(있으면)로 폴백한다.
	 */
	public List<String> selectDistricts(
		final TravelProfile profile,
		final TravelRegion region,
		final Set<TourApiContentType> contentTypes
	) {
		if (contentTypes.isEmpty() || region == null) {
			return List.of();
		}

		Map<String, Double> ratios = localityScoreCalculator.districtLocalRatios(region.getLDongRegnCd());
		if (ratios.isEmpty()) {
			return region.getLDongSignguCd() == null || region.getLDongSignguCd().isBlank()
				? List.of() : List.of(region.getLDongSignguCd());
		}

		double target = profile.targetLocality();
		int needed = Math.min(MAX_DISTRICTS, Math.max(1, profile.requiredDistrictCount()));

		// 로컬도 목표에 가장 가까운 시군구를 앵커로
		String anchor = ratios.entrySet().stream()
			.min(Comparator.comparingDouble(e -> Math.abs(e.getValue() - target)))
			.map(Map.Entry::getKey)
			.orElseThrow();

		String regionCode = region.getLDongRegnCd();
		GeoPoint anchorCentroid = districtCentroidProvider.centroid(regionCode + anchor);

		List<String> selected = new ArrayList<>();
		selected.add(anchor);

		if (anchorCentroid == null) {
			// 앵커 좌표 미확보: 로컬도 근접 순 폴백
			ratios.keySet().stream()
				.filter(d -> !d.equals(anchor))
				.sorted(Comparator.comparingDouble(d -> Math.abs(ratios.get(d) - target)))
				.limit(Math.max(0, needed - 1))
				.forEach(selected::add);
			return selected;
		}

		// 앵커에서 clusterRadiusKm 이내인 시군구만, 가까운 순으로 선정(먼 외딴 시군구 배제).
		// 좌표가 없는 시군구는 제외한다.
		double radiusKm = profile.clusterRadiusKm();
		ratios.keySet().stream()
			.filter(d -> !d.equals(anchor))
			.filter(d -> districtCentroidProvider.centroid(regionCode + d) != null)
			.filter(d -> GeoUtils.haversineKm(
				anchorCentroid, districtCentroidProvider.centroid(regionCode + d)) <= radiusKm)
			.sorted(Comparator.comparingDouble(d -> GeoUtils.haversineKm(
				anchorCentroid, districtCentroidProvider.centroid(regionCode + d))))
			.limit(Math.max(0, needed - 1))
			.forEach(selected::add);
		return selected;
	}

	/**
	 * 선정된 시군구 × 콘텐츠 유형별로 TourAPI(on-demand)에서 후보를 병렬 조회·동기화하고,
	 * 확보된 placeId로 Place 엔티티를 로드해 반환한다.
	 * 각 조합은 독립 트랜잭션(워커)에서 커밋되므로, 이 메서드 자체는 트랜잭션이 아니다.
	 */
	public List<Place> collectCandidates(
		final TravelRegion region,
		final List<String> districtCodes,
		final Set<TourApiContentType> contentTypes,
		final LocalDate tripStart,
		final LocalDate tripEnd,
		final PlaceLocale locale
	) {
		if (region == null || districtCodes.isEmpty() || contentTypes.isEmpty()) {
			return List.of();
		}

		PlaceLocale displayLocale = locale == null ? PlaceLocale.KO : locale;
		String regionCode = region.getLDongRegnCd();

		List<CompletableFuture<List<Long>>> futures = new ArrayList<>();
		for (String districtCode : districtCodes) {
			for (TourApiContentType contentType : contentTypes) {
				futures.add(CompletableFuture.supplyAsync(
					() -> candidateFetchWorker.fetchPlaceIds(
						regionCode, districtCode, contentType, tripStart, tripEnd,
						displayLocale, PAGE_SIZE, MAX_PAGES),
					candidateFetchExecutor));
			}
		}

		Set<Long> placeIds = new LinkedHashSet<>();
		for (CompletableFuture<List<Long>> future : futures) {
			placeIds.addAll(future.join());
		}

		if (placeIds.isEmpty()) {
			return List.of();
		}

		// 하드 제외: 어느 로케일 이름이든 차단 키워드(예: '노브랜드')가 포함되면 후보에서 뺀다.
		// (EN/JA TourAPI 이름에도 한국어가 괄호로 붙어 오므로 KO만 검사하면 놓친다.)
		Set<Long> blocked = localizedContentRepository
			.findAllByPlaceIdInAndVisibleTrue(placeIds)
			.stream()
			.filter(content -> containsBlockedKeyword(content.getName()))
			.map(content -> content.getPlace().getId())
			.collect(Collectors.toSet());

		return placeRepository.findAllById(placeIds).stream()
			.filter(place -> !blocked.contains(place.getId()))
			.toList();
	}

	private boolean containsBlockedKeyword(final String name) {
		if (name == null) {
			return false;
		}
		return BLOCKED_NAME_KEYWORDS.stream().anyMatch(name::contains);
	}
}
