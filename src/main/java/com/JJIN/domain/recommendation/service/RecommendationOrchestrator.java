package com.JJIN.domain.recommendation.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;
import com.JJIN.domain.place.entity.Place;
import com.JJIN.domain.recommendation.dto.CourseDraft;
import com.JJIN.domain.recommendation.dto.RecommendationCandidate;
import com.JJIN.domain.recommendation.dto.ScoredCandidate;
import com.JJIN.domain.recommendation.dto.TravelProfile;
import com.JJIN.domain.recommendation.slot.SlotCourseAssembler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 여행 추천 파이프라인 진입점.
 * TravelPlan을 받아 프로파일 정규화 → 시군구 선정 → 후보 조회 → 하드 필터 → 스코어링 →
 * 슬롯 기반 코스 조립 순으로 실행해 일자별 코스 초안을 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationOrchestrator {

	// 여행 규모에 맞춰 조립기에 넘길 후보 수를 동적으로 정한다.
	// 목표 후보 = 여행일수 × 하루 슬롯 × 여유배수 를 선택 카테고리 수로 나눠 유형별 상한을 구한다.
	// 조립이 결정론적(비 LLM) 알고리즘이라 후보를 넉넉히 넘겨도 비용이 거의 없으므로 상한을 크게 잡는다.
	private static final double CANDIDATE_OVERSAMPLE = 2.5;
	private static final int MIN_CANDIDATES_PER_ROLE = 30;
	private static final int MAX_CANDIDATES_PER_ROLE = 80;

	// 조립 전, 각 날 콘텐츠 유형별로 운영시간을 미리 조회(enrich)할 상위 후보 수.
	// 모든 시간민감 슬롯(식사·카페·관광·문화·쇼핑)이 '방문 시각에 열림' 판정을 할 수 있게 한다.
	private static final int ENRICH_CANDIDATES_PER_DAY_PER_TYPE = 6;

	// 사용자 선택과 무관하게 항상 후보로 포함하는 기본 카테고리(관광지·쇼핑·음식점·문화시설).
	// 선택하지 않은 취향도 코스에 섞여 카테고리 편중(예: 음식점만 선택 시 전부 음식점)을 막는다.
	private static final Set<TourApiContentType> BASE_CATEGORIES = Set.of(
		TourApiContentType.TOURIST_ATTRACTION,
		TourApiContentType.SHOPPING,
		TourApiContentType.RESTAURANT,
		TourApiContentType.CULTURAL_FACILITY);
	// 선택하지 않은 카테고리도 코스 변화를 위해 넉넉히 넘긴다(조립기가 슬롯 수만큼만 실제 사용).
	private static final int UNSELECTED_CANDIDATES = 15;
	// 음식점은 매 끼니 필요하고 운영시간 필터로 걸러지므로, 선택하지 않았어도 더 넉넉히 넘긴다.
	private static final int RESTAURANT_UNSELECTED_CANDIDATES = 20;

	private final TravelProfileNormalizer profileNormalizer;
	private final CandidatePoolService candidatePoolService;
	private final RecommendationCandidateAssembler candidateAssembler;
	private final CandidateFilterService filterService;
	private final PlaceScoringService scoringService;
	private final PreferredCategoryResolver categoryResolver;
	private final RecommendationHistoryStore historyStore;
	private final CourseClusterer courseClusterer;
	private final OperatingInfoEnricher operatingInfoEnricher;
	private final SlotCourseAssembler slotCourseAssembler;

	/**
	 * TravelPlan 기준으로 P0~P4를 실행해 LLM 코스 초안을 반환한다.
	 * memberId는 추천이력 패널티 계산에 사용한다.
	 */
	@Transactional
	public CourseDraft recommend(final TravelPlan plan, final Long memberId, final PlaceLocale locale) {
		// P0: 여행 프로파일 정규화
		TravelProfile profile = profileNormalizer.normalize(plan);
		log.info("추천 시작: planId={}, slots={}, districts={}, level={}",
			plan.getId(), profile.slotCount(), profile.requiredDistrictCount(), profile.experienceLevel());

		// P1: 사용자 선호 카테고리 추출 + 시군구 선정 + 후보 조회
		Set<TravelSubcategory> subcategories = plan.getPreferences().stream()
			.map(pref -> pref.getSubcategory())
			.collect(Collectors.toSet());
		Set<TourApiContentType> selectedTypes = TravelSubcategory.toContentTypes(subcategories)
			.stream()
			.filter(TourApiContentType::isPreferenceSelectable)
			.collect(Collectors.toSet());

		// 기본 4개 카테고리는 항상 포함하고 선택 카테고리를 합쳐 후보를 조회한다.
		Set<TourApiContentType> contentTypes = new HashSet<>(BASE_CATEGORIES);
		contentTypes.addAll(selectedTypes);

		List<String> districts = candidatePoolService.selectDistricts(profile, plan.getRegion(), contentTypes);
		if (districts.isEmpty()) {
			log.warn("선정된 시군구가 없습니다: planId={}", plan.getId());
			return null;
		}

		List<Place> places = candidatePoolService.collectCandidates(
			plan.getRegion(), districts, contentTypes, plan.getStartDate(), plan.getEndDate(), locale);
		log.info("후보 조회: planId={}, rawCount={}", plan.getId(), places.size());

		// P2: Place + 운영정보 결합 → 하드 필터
		List<RecommendationCandidate> candidates = candidateAssembler.assemble(places);
		List<RecommendationCandidate> filtered = filterService.applyHardFilters(
			candidates, profile, plan.getStartDate(), plan.getEndDate());
		log.info("하드 필터 후: planId={}, filteredCount={}", plan.getId(), filtered.size());

		// P3: 스코어링 → 역할별 상위 N개 선택
		Set<String> preferredMidCodes = categoryResolver.resolveMidCategoryCodes(subcategories);
		Set<Long> recentlyRecommended = historyStore.findRecentlyRecommended(
			memberId, filtered.stream().map(RecommendationCandidate::placeId).collect(Collectors.toSet()));

		List<ScoredCandidate> scored = filtered.stream()
			.map(candidate -> scoringService.score(
				candidate, profile, preferredMidCodes, recentlyRecommended,
				plan.getStartDate(), plan.getEndDate()))
			.collect(Collectors.toList());

		int perRole = candidatesPerRole(profile, selectedTypes.size());
		Map<TourApiContentType, Integer> perRoleLimits = candidateLimits(contentTypes, selectedTypes, perRole);
		List<ScoredCandidate> top = scoringService.selectTopByRole(scored, perRoleLimits, UNSELECTED_CANDIDATES);
		log.info("스코어링 완료: planId={}, perRole={}, topCount={}", plan.getId(), perRole, top.size());

		// P4: 슬롯 기반 코스 조립 (LLM 없이 결정론적으로 배치)
		Map<Long, Integer> dayByPlaceId = courseClusterer.clusterByDay(top, profile.tripDays());
		// 모든 시간민감 슬롯의 '방문 시각에 열림' 판정을 위해, 각 날·유형별 상위 후보 운영시간을 미리 채운다.
		operatingInfoEnricher.enrichAll(timeSensitiveEnrichTargets(top, dayByPlaceId, profile), locale);
		CourseDraft draft = slotCourseAssembler.assemble(
			top, dayByPlaceId, profile, plan.getStartDate(), selectedTypes);
		log.info("코스 초안 생성: planId={}, success={}", plan.getId(), draft != null);

		// 최종 코스에 선정된 방문지의 운영시간도 마저 채운다(조립 전 enrich에서 빠진 곳 보완, 캐시 재사용).
		if (draft != null) {
			operatingInfoEnricher.enrichAll(selectedPlaceIds(draft), locale);
		}

		return draft;
	}

	/**
	 * 조립 전 운영시간을 미리 채울 후보 placeId 집합.
	 * 각 (일차, 콘텐츠 유형)별 점수 상위 {@link #ENRICH_CANDIDATES_PER_DAY_PER_TYPE}개를 모은다.
	 */
	private Set<Long> timeSensitiveEnrichTargets(
		final List<ScoredCandidate> top,
		final Map<Long, Integer> dayByPlaceId,
		final TravelProfile profile
	) {
		Set<Long> targets = new HashSet<>();
		for (int day = 1; day <= profile.tripDays(); day++) {
			final int currentDay = day;
			Map<TourApiContentType, List<ScoredCandidate>> byType = top.stream()
				.filter(s -> dayByPlaceId.getOrDefault(s.candidate().placeId(), 1) == currentDay)
				.collect(Collectors.groupingBy(s -> s.candidate().contentType()));
			byType.values().forEach(group -> group.stream()
				.sorted((a, b) -> Double.compare(b.finalScore(), a.finalScore()))
				.limit(ENRICH_CANDIDATES_PER_DAY_PER_TYPE)
				.forEach(s -> targets.add(s.candidate().placeId())));
		}
		return targets;
	}

	private Set<Long> selectedPlaceIds(final CourseDraft draft) {
		return draft.days().stream()
			.filter(day -> day.visits() != null)
			.flatMap(day -> day.visits().stream())
			.map(CourseDraft.PlannedVisit::placeId)
			.collect(Collectors.toSet());
	}

	/**
	 * 여행 규모에 비례해 선택 카테고리별 후보 상한을 계산한다.
	 * (여행일수 × 하루 슬롯 × 여유배수) / 선택 유형 수 를 [MIN, MAX]로 clamp.
	 */
	private int candidatesPerRole(final TravelProfile profile, final int selectedTypeCount) {
		int roles = Math.max(1, selectedTypeCount);
		int targetTotal = (int) Math.ceil(profile.tripDays() * profile.slotCount() * CANDIDATE_OVERSAMPLE);
		int perRole = (int) Math.ceil((double) targetTotal / roles);
		return Math.max(MIN_CANDIDATES_PER_ROLE, Math.min(MAX_CANDIDATES_PER_ROLE, perRole));
	}

	/**
	 * 카테고리별 LLM 전달 후보 상한을 정한다.
	 * - 사용자가 선택한 카테고리: 여행 규모 비례 상한(perRole)
	 * - 선택하지 않은 음식점: RESTAURANT_UNSELECTED_CANDIDATES(끼니 보장)
	 * - 그 외 선택하지 않은 카테고리: UNSELECTED_CANDIDATES
	 */
	private Map<TourApiContentType, Integer> candidateLimits(
		final Set<TourApiContentType> contentTypes,
		final Set<TourApiContentType> selectedTypes,
		final int perRole
	) {
		Map<TourApiContentType, Integer> limits = new HashMap<>();
		for (TourApiContentType type : contentTypes) {
			if (selectedTypes.contains(type)) {
				limits.put(type, perRole);
			} else if (type == TourApiContentType.RESTAURANT) {
				limits.put(type, RESTAURANT_UNSELECTED_CANDIDATES);
			} else {
				limits.put(type, UNSELECTED_CANDIDATES);
			}
		}
		return limits;
	}
}
