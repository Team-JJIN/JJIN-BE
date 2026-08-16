package com.JJIN.domain.mission.dto.response;

import java.util.List;

public record AddMissionToPlansResponse(
	List<LikeItem> likes
) {

	public record LikeItem(
		Long likeId,
		Long planId
	) {
	}

	public static AddMissionToPlansResponse of(final List<LikeItem> likes) {
		return new AddMissionToPlansResponse(likes);
	}
}
