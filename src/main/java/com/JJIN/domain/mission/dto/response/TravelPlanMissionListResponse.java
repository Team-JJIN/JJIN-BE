package com.JJIN.domain.mission.dto.response;

import java.util.List;

import com.JJIN.domain.onboarding.entity.TravelPlan;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일정 미션 목록 응답")
public record TravelPlanMissionListResponse(

	@Schema(description = "여행 일정 ID", example = "1")
	Long travelPlanId,

	@Schema(description = "여행 일정명", example = "여름 마카오 여행")
	String travelPlanName,

	@Schema(description = "전체 미션 수", example = "3")
	long totalCount,

	@Schema(description = "인증 필요 미션 수", example = "1")
	long proofRequiredCount,

	@Schema(description = "피드 업로드 대기 미션 수", example = "1")
	long uploadPendingCount,

	@Schema(description = "완료 미션 수", example = "1")
	long completedCount,

	@Schema(description = "선택한 상태에 해당하는 미션 목록")
	List<TravelPlanMissionItemResponse> missions
) {

	public static TravelPlanMissionListResponse of(
		final TravelPlan travelPlan,
		final long totalCount,
		final long proofRequiredCount,
		final long uploadPendingCount,
		final long completedCount,
		final List<TravelPlanMissionItemResponse> missions
	) {
		return new TravelPlanMissionListResponse(
			travelPlan.getId(),
			travelPlan.getName(),
			totalCount,
			proofRequiredCount,
			uploadPendingCount,
			completedCount,
			missions
		);
	}
}
