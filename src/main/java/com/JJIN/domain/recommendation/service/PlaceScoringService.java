package com.JJIN.domain.recommendation.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.JJIN.domain.place.dto.WeeklySchedule;
import com.JJIN.domain.place.entity.enums.OperatingInfoParseStatus;
import com.JJIN.domain.recommendation.dto.RecommendationCandidate;
import com.JJIN.domain.recommendation.dto.ScoredCandidate;
import com.JJIN.domain.recommendation.dto.TravelProfile;

import lombok.RequiredArgsConstructor;

/**
 * 장소 스코어링. 후보별 CategoryFit·LocalityFit·TimeFit에 레벨 가중치와 패널티를 적용해 최종 점수를 매긴다.
 */
@Service
@RequiredArgsConstructor
public class PlaceScoringService {

	private static final double NEUTRAL_LOCALITY = 0.5;

	private static final double CATEGORY_MID_MATCH = 1.0;
	private static final double CATEGORY_CONTENT_ONLY = 0.4;

	private static final double TIME_FULL = 1.0;
	private static final double TIME_BOUNDARY = 0.6;
	private static final double TIME_UNKNOWN = 0.3;

	private static final int PENALTY_TIME_UNKNOWN = 10;
	private static final int PENALTY_RECENTLY_RECOMMENDED = 20;

	private final OpenStatusCalculator openStatusCalculator;

	public ScoredCandidate score(
		final RecommendationCandidate candidate,
		final TravelProfile profile,
		final Set<String> preferredMidCategoryCodes,
		final Set<Long> recentlyRecommendedPlaceIds,
		final LocalDate tripStart,
		final LocalDate tripEnd
	) {
		double categoryFit = categoryFit(candidate, preferredMidCategoryCodes);
		double localityFit = localityFit(candidate, profile.targetLocality());
		double timeFit = timeFit(candidate, profile, tripStart, tripEnd);

		double base = 100.0 * (
			profile.categoryWeight() * categoryFit
				+ profile.localityWeight() * localityFit
				+ profile.timeWeight() * timeFit);

		int penalty = 0;
		if (timeFit == TIME_UNKNOWN) {
			penalty += PENALTY_TIME_UNKNOWN;
		}
		if (recentlyRecommendedPlaceIds.contains(candidate.placeId())) {
			penalty += PENALTY_RECENTLY_RECOMMENDED;
		}

		return new ScoredCandidate(candidate, categoryFit, localityFit, timeFit, base - penalty);
	}

	/**
	 * 역할(콘텐츠 유형)별로 최종 점수 상위 N개만 남긴다.
	 */
	public List<ScoredCandidate> selectTopByRole(final List<ScoredCandidate> scored, final int perRoleLimit) {
		return scored.stream()
			.collect(Collectors.groupingBy(s -> s.candidate().contentType()))
			.values().stream()
			.flatMap(group -> group.stream()
				.sorted(Comparator.comparingDouble(ScoredCandidate::finalScore).reversed())
				.limit(perRoleLimit))
			.toList();
	}

	/** 중분류(lclsSystm2) 일치면 1.0, 아니면 콘텐츠 유형만 일치로 보아 0.4 */
	private double categoryFit(final RecommendationCandidate candidate, final Set<String> preferredMidCategoryCodes) {
		if (candidate.lclsSystm2Code() != null && preferredMidCategoryCodes.contains(candidate.lclsSystm2Code())) {
			return CATEGORY_MID_MATCH;
		}
		return CATEGORY_CONTENT_ONLY;
	}

	/** 목표 로컬도와 장소 로컬도의 근접도. 로컬도 미상이면 중립값(0.5)으로 근사 */
	private double localityFit(final RecommendationCandidate candidate, final double targetLocality) {
		double placeLocality = candidate.localityScore() == null ? NEUTRAL_LOCALITY : candidate.localityScore();
		return 1.0 - Math.abs(targetLocality - placeLocality);
	}

	/** 운영시간이 활동 시간대를 완전히 포함하면 1.0, 겹치기만 하면 0.6, 미상이면 0.3 */
	private double timeFit(
		final RecommendationCandidate candidate,
		final TravelProfile profile,
		final LocalDate tripStart,
		final LocalDate tripEnd
	) {
		WeeklySchedule schedule = candidate.weeklySchedule();
		boolean known = schedule != null
			&& (candidate.parseStatus() == OperatingInfoParseStatus.PARSED
			|| candidate.parseStatus() == OperatingInfoParseStatus.PARTIAL);
		if (!known) {
			return TIME_UNKNOWN;
		}

		boolean covers = false;
		boolean overlaps = false;
		for (LocalDate date = tripStart; !date.isAfter(tripEnd); date = date.plusDays(1)) {
			if (openStatusCalculator.coversActivityWindow(
				schedule, date.getDayOfWeek(), profile.activityStartTime(), profile.activityEndTime())) {
				covers = true;
				break;
			}
			if (openStatusCalculator.overlapsActivityWindow(
				schedule, date.getDayOfWeek(), profile.activityStartTime(), profile.activityEndTime())) {
				overlaps = true;
			}
		}

		if (covers) {
			return TIME_FULL;
		}
		return overlaps ? TIME_BOUNDARY : TIME_UNKNOWN;
	}
}
