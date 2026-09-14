package com.JJIN.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.JJIN.domain.mission.entity.UserMission;
import com.JJIN.domain.mission.entity.enums.UserMissionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일정 미션 사진 인증 정보")
public record TravelPlanMissionAuthenticationResponse(
	Long travelPlanId,
	Long userMissionId,
	Long missionId,
	@Schema(description = "미션 원본 제목. 피드 제목과는 별개", example = "아인슈페너 사먹기")
	String missionTitle,
	UserMissionStatus status,
	@Schema(description = "DB에 저장된 인증 사진 S3 객체 key. 인증 전에는 null", nullable = true)
	String proofImageKey,
	@Schema(description = "인증 사진 표시용 Presigned GET URL. 유효시간 1시간, 인증 전에는 null", nullable = true)
	String proofImageUrl,
	@Schema(description = "사진 인증 시각. 인증 전에는 null", nullable = true)
	LocalDateTime authenticatedAt
) {
	public static TravelPlanMissionAuthenticationResponse of(
		final UserMission userMission,
		final String proofImageUrl
	) {
		return new TravelPlanMissionAuthenticationResponse(
			userMission.getTravelPlan().getId(),
			userMission.getId(),
			userMission.getMission().getId(),
			userMission.getMission().getTitle(),
			userMission.getStatus(),
			userMission.getProofImageKey(),
			proofImageUrl,
			userMission.getAuthenticatedAt()
		);
	}
}
