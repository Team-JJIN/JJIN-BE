package com.JJIN.domain.mission.dto.response;

public record CreateMissionResponse(
	Long missionId
) {

	public static CreateMissionResponse of(final Long missionId) {
		return new CreateMissionResponse(missionId);
	}
}
