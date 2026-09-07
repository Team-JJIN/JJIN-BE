package com.JJIN.domain.travelplan.dto.response;

import java.util.List;

import com.JJIN.domain.onboarding.entity.TravelPlan;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "여행 일정 목록 조회 응답")
public record TravelPlanListResponse(
	int totalCount,
	List<TravelPlanSummaryResponse> travelPlans
) {

	public static TravelPlanListResponse from(final List<TravelPlan> travelPlans) {
		return new TravelPlanListResponse(
			travelPlans.size(),
			travelPlans.stream()
				.map(TravelPlanSummaryResponse::from)
				.toList()
		);
	}
}
