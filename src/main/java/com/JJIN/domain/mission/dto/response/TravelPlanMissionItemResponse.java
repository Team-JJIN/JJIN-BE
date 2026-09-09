package com.JJIN.domain.mission.dto.response;

import com.JJIN.domain.mission.entity.UserMission;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.mission.entity.enums.UserMissionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일정에 담긴 미션 카드 응답")
public record TravelPlanMissionItemResponse(

	@Schema(description = "일정 미션 ID", example = "25")
	Long userMissionId,

	@Schema(description = "미션 ID", example = "7")
	Long missionId,

	@Schema(description = "미션명", example = "아인슈페너 사먹기")
	String title,

	@Schema(description = "미션 설명", example = "시그니처 아인슈페너 주문하고 인증샷 남기기")
	String description,

	@Schema(description = "미션 이미지 URL", nullable = true)
	String imageUrl,

	@Schema(description = "미션 난이도", example = "ONE")
	MissionDifficulty difficulty,

	@Schema(description = "진행 상태", example = "PROOF_REQUIRED")
	UserMissionStatus status,

	@Schema(description = "완료한 최신 인증글 ID. 완료 전이거나 인증글이 없으면 null", example = "81", nullable = true)
	Long missionProofId
) {

	public static TravelPlanMissionItemResponse of(
		final UserMission userMission,
		final Long missionProofId
	) {
		return new TravelPlanMissionItemResponse(
			userMission.getId(),
			userMission.getMission().getId(),
			userMission.getMission().getTitle(),
			userMission.getMission().getDescription(),
			userMission.getMission().getImageUrl(),
			userMission.getMission().getDifficulty(),
			userMission.getStatus(),
			missionProofId
		);
	}
}
