package com.JJIN.domain.recommendation.dto;

import java.util.List;

/**
 * 추천 파이프라인이 조립한 일자별 코스 초안.
 */
public record CourseDraft(
	List<DayPlan> days
) {

	public record DayPlan(
		int dayNumber,
		List<PlannedVisit> visits
	) {
	}

	public record PlannedVisit(
		Long placeId,
		String suggestedStartTime,
		String suggestedEndTime,
		int stayMinutes
	) {
	}
}
