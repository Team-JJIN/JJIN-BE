package com.JJIN.domain.recommendation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.enums.OpenStatus;
import com.JJIN.domain.recommendation.dto.CourseDraft;
import com.JJIN.domain.recommendation.dto.CourseDraft.DayPlan;
import com.JJIN.domain.recommendation.dto.CourseDraft.PlannedVisit;
import com.JJIN.domain.recommendation.dto.RecommendationCandidate;
import com.JJIN.domain.recommendation.dto.TravelProfile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 코스 검증. LLM 코스 초안이 제약을 지키는지 확인하고 위반 사유 목록을 반환한다.
 * 위반이 있으면 오케스트레이터가 위반 내용을 담아 LLM 재생성을 요청한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseValidationService {

	private final OpenStatusCalculator openStatusCalculator;
	private final CourseTravelTimeResolver travelTimeResolver;

	public List<String> verify(
		final CourseDraft draft,
		final Map<Long, RecommendationCandidate> candidatesById,
		final TravelProfile profile,
		final LocalDate tripStart
	) {
		List<String> violations = new ArrayList<>();
		if (draft == null || draft.days() == null || draft.days().isEmpty()) {
			violations.add("코스 초안이 비어 있습니다.");
			return violations;
		}

		Set<Long> visitedPlaceIds = new HashSet<>();
		for (DayPlan day : draft.days()) {
			verifyDay(day, candidatesById, profile, tripStart, visitedPlaceIds, violations);
		}
		return violations;
	}

	private void verifyDay(
		final DayPlan day,
		final Map<Long, RecommendationCandidate> candidatesById,
		final TravelProfile profile,
		final LocalDate tripStart,
		final Set<Long> visitedPlaceIds,
		final List<String> violations
	) {
		List<PlannedVisit> visits = day.visits() == null ? List.of() : day.visits();
		LocalDate visitDate = tripStart.plusDays(Math.max(0, day.dayNumber() - 1));

		TourApiContentType previousType = null;
		RecommendationCandidate previous = null;

		for (PlannedVisit visit : visits) {
			RecommendationCandidate candidate = candidatesById.get(visit.placeId());
			if (candidate == null) {
				violations.add("day%d: 후보 밖 placeId=%d".formatted(day.dayNumber(), visit.placeId()));
				continue;
			}

			if (!visitedPlaceIds.add(visit.placeId())) {
				violations.add("중복 방문: placeId=%d".formatted(visit.placeId()));
			}

			if (previousType != null && previousType == candidate.contentType()) {
				violations.add("day%d: 카테고리 연속 배치(%s)".formatted(day.dayNumber(), candidate.contentType()));
			}

			verifyOperatingAndWindow(day, visitDate, visit, candidate, profile, violations);
			verifyLegBudget(day, previous, candidate, profile, violations);

			previousType = candidate.contentType();
			previous = candidate;
		}
	}

	private void verifyOperatingAndWindow(
		final DayPlan day,
		final LocalDate visitDate,
		final PlannedVisit visit,
		final RecommendationCandidate candidate,
		final TravelProfile profile,
		final List<String> violations
	) {
		LocalTime start = parseTime(visit.suggestedStartTime());
		LocalTime end = parseTime(visit.suggestedEndTime());

		if (start != null && start.isBefore(profile.activityStartTime())) {
			violations.add("day%d: placeId=%d 방문 시작이 활동 시작 이전".formatted(day.dayNumber(), visit.placeId()));
		}
		if (end != null && end.isAfter(profile.activityEndTime())) {
			violations.add("day%d: placeId=%d 방문 종료가 활동 종료 이후".formatted(day.dayNumber(), visit.placeId()));
		}

		if (start != null && candidate.weeklySchedule() != null) {
			OpenStatus status = openStatusCalculator.calculate(
				candidate.weeklySchedule(), LocalDateTime.of(visitDate, start));
			if (status == OpenStatus.CLOSED || status == OpenStatus.BREAK) {
				violations.add("day%d: placeId=%d 운영시간 밖 방문(%s)"
					.formatted(day.dayNumber(), visit.placeId(), status));
			}
		}
	}

	private void verifyLegBudget(
		final DayPlan day,
		final RecommendationCandidate previous,
		final RecommendationCandidate current,
		final TravelProfile profile,
		final List<String> violations
	) {
		if (previous == null) {
			return;
		}
		int legMinutes = travelTimeResolver.estimateMinutes(
			previous.location(), current.location(), profile.transportMode());
		if (legMinutes > profile.legBudgetMinutes()) {
			violations.add("day%d: 구간 이동시간 초과 placeId=%d→%d (%d분 > 예산 %d분)"
				.formatted(day.dayNumber(), previous.placeId(), current.placeId(),
					legMinutes, profile.legBudgetMinutes()));
		}
	}

	private LocalTime parseTime(final String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		try {
			return LocalTime.parse(text.trim());
		} catch (Exception e) {
			return null;
		}
	}
}
