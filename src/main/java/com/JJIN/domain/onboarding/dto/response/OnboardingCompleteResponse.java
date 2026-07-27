package com.JJIN.domain.onboarding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "온보딩 완료 응답")
public record OnboardingCompleteResponse(

	@Schema(description = "온보딩으로 생성된 여행 일정 ID", example = "1")
	Long travelPlanId
) {

	public static OnboardingCompleteResponse of(final Long travelPlanId) {
		return new OnboardingCompleteResponse(travelPlanId);
	}
}
