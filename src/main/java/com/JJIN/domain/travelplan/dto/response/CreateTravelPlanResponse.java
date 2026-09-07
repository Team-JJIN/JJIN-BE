package com.JJIN.domain.travelplan.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "여행 일정 생성 응답")
public record CreateTravelPlanResponse(

	@Schema(description = "생성된 여행 일정 ID", example = "1")
	Long travelPlanId
) {

	public static CreateTravelPlanResponse of(final Long travelPlanId) {
		return new CreateTravelPlanResponse(travelPlanId);
	}
}
