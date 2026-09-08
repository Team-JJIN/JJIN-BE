package com.JJIN.domain.recommendation.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;
import com.JJIN.domain.place.entity.Place;
import com.JJIN.domain.recommendation.dto.CourseDraft;
import com.JJIN.domain.recommendation.dto.RecommendationCandidate;
import com.JJIN.domain.recommendation.dto.ScoredCandidate;
import com.JJIN.domain.recommendation.dto.TravelProfile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 여행 추천 파이프라인 진입점.
 * TravelPlan을 받아 프로파일 정규화 → 시군구 선정 → 후보 조회 → 하드 필터 → 스코어링 순으로 실행하고,
 * LLM에 전달할 역할별 상위 후보 목록을 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationOrchestrator {

	private static final int TOP_CANDIDATES_PER_ROLE = 5;

	private final TravelProfileNormalizer profileNormalizer;
	private final CandidatePoolService candidatePoolService;
	private final RecommendationCandidateAssembler candidateAssembler;
	private final CandidateFilterService filterService;
	private final PlaceScoringService scoringService;
	private final PreferredCategoryResolver categoryResolver;
	private final RecommendationHistoryStore historyStore;
	private final CourseAssemblyService courseAssemblyService;
	private final CourseValidationService courseValidationService;

	/**
	 * TravelPlan 기준으로 P0~P4를 실행해 LLM 코스 초안을 반환한다.
	 * memberId는 추천이력 패널티 계산에 사용한다.
	 */
	@Transactional(readOnly = true)
	public CourseDraft recommend(final TravelPlan plan, final Long memberId) {
		// P0: 여행 프로파일 정규화
		TravelProfile profile = profileNormalizer.normalize(plan);
		log.info("추천 시작: planId={}, slots={}, districts={}, level={}",
			plan.getId(), profile.slotCount(), profile.requiredDistrictCount(), profile.experienceLevel());

		// P1: 사용자 선호 카테고리 추출 + 시군구 선정 + 후보 조회
		Set<TravelSubcategory> subcategories = plan.getPreferences().stream()
			.map(pref -> pref.getSubcategory())
			.collect(Collectors.toSet());
		Set<TourApiContentType> contentTypes = TravelSubcategory.toContentTypes(subcategories)
			.stream()
			.filter(TourApiContentType::isPreferenceSelectable)
			.collect(Collectors.toSet());

		if (contentTypes.isEmpty()) {
			log.warn("선택된 콘텐츠 유형이 없어 빈 결과를 반환합니다: planId={}", plan.getId());
			return null;
		}

		List<String> districts = candidatePoolService.selectDistricts(profile, plan.getRegion(), contentTypes);
		if (districts.isEmpty()) {
			log.warn("선정된 시군구가 없습니다: planId={}", plan.getId());
			return null;
		}

		List<Place> places = candidatePoolService.collectCandidates(
			plan.getRegion(), districts, contentTypes, plan.getStartDate(), plan.getEndDate());
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

		List<ScoredCandidate> top = scoringService.selectTopByRole(scored, TOP_CANDIDATES_PER_ROLE);
		log.info("스코어링 완료: planId={}, topCount={}", plan.getId(), top.size());

		// P4: LLM 코스 조립
		CourseDraft draft = courseAssemblyService.assemble(top, profile, plan.getStartDate(), plan.getEndDate());
		log.info("코스 초안 생성: planId={}, success={}", plan.getId(), draft != null);

		// P5: 검증 → 위반 시 위반 내용을 담아 1회 재생성
		if (draft != null) {
			draft = validateAndReassemble(draft, top, profile, plan);
		}

		return draft;
	}

	private CourseDraft validateAndReassemble(
		final CourseDraft draft,
		final List<ScoredCandidate> top,
		final TravelProfile profile,
		final TravelPlan plan
	) {
		Map<Long, RecommendationCandidate> candidatesById = top.stream()
			.collect(Collectors.toMap(s -> s.candidate().placeId(), ScoredCandidate::candidate, (a, b) -> a));

		List<String> violations = courseValidationService.verify(
			draft, candidatesById, profile, plan.getStartDate());
		if (violations.isEmpty()) {
			return draft;
		}

		log.warn("코스 검증 위반 {}건, 재생성 시도: planId={}", violations.size(), plan.getId());
		CourseDraft retried = courseAssemblyService.reassemble(
			top, profile, plan.getStartDate(), plan.getEndDate(), violations);
		if (retried == null) {
			return draft;
		}

		List<String> retryViolations = courseValidationService.verify(
			retried, candidatesById, profile, plan.getStartDate());
		// 재생성 결과가 더 낫거나 같으면 채택
		return retryViolations.size() <= violations.size() ? retried : draft;
	}
}
