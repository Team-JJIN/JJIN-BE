package com.JJIN.domain.mission.dto.response;

import com.JJIN.domain.mission.entity.MissionProof;
import com.JJIN.domain.mission.entity.UserMission;
import com.JJIN.domain.mission.entity.enums.UserMissionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 인증 피드 업로드 결과")
public record TravelPlanMissionFeedUploadResponse(

	@Schema(description = "생성된 미션 인증글(피드) ID", example = "81")
	Long missionProofId,

	@Schema(description = "일정 미션 ID", example = "25")
	Long userMissionId,

	@Schema(description = "미션 ID", example = "7")
	Long missionId,

	@Schema(description = "업로드 후 일정 미션 상태 (COMPLETED)", example = "COMPLETED")
	UserMissionStatus status,

	@Schema(description = "DB에 저장된 인증 사진의 공개 raw URL")
	String imageUrl,

	@Schema(description = "피드에 올린 인증 문구", nullable = true)
	String content
) {
	public static TravelPlanMissionFeedUploadResponse of(
		final MissionProof proof,
		final UserMission userMission,
		final String imageUrl
	) {
		return new TravelPlanMissionFeedUploadResponse(
			proof.getId(),
			userMission.getId(),
			userMission.getMission().getId(),
			userMission.getStatus(),
			imageUrl,
			proof.getContent()
		);
	}
}
