package com.JJIN.domain.mission.dto.response;

import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 인증글에 연결된 미션 요약 정보")
public record MissionProofMissionSummaryResponse(

	@Schema(description = "미션 ID", example = "10")
	Long missionId,

	@Schema(description = "미션 제목", example = "매머드 커피 아인슈페너 사먹기")
	String title,

	@Schema(description = "난이도", example = "ONE")
	MissionDifficulty difficulty,

	@Schema(description = "이번 주 완료 수", example = "128")
	long weeklyCompletedCount
) {

	public static MissionProofMissionSummaryResponse of(
		final Mission mission,
		final long weeklyCompletedCount
	) {
		MissionDifficulty difficulty = mission.getDifficulty();
		return new MissionProofMissionSummaryResponse(
			mission.getId(),
			mission.getTitle(),
			difficulty,
			weeklyCompletedCount
		);
	}
}
