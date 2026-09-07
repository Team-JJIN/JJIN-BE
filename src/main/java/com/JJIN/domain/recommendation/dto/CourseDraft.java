package com.JJIN.domain.recommendation.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LLM이 생성한 일자별 코스 초안.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CourseDraft(
	List<DayPlan> days
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record DayPlan(
		int dayNumber,
		List<PlannedVisit> visits
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record PlannedVisit(
		Long placeId,
		String suggestedStartTime,
		String suggestedEndTime,
		int stayMinutes,
		String reason
	) {
	}
}
