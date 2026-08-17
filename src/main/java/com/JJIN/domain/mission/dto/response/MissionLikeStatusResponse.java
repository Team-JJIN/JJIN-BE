package com.JJIN.domain.mission.dto.response;

import java.time.LocalDate;
import java.util.List;

public record MissionLikeStatusResponse(
	int totalPlans,
	List<PlanLikeItem> likes
) {

	public record PlanLikeItem(
		Long planId,
		String planName,
		LocalDate planStartDate,
		LocalDate planEndDate,
		boolean isLiked,
		Long likeId
	) {
	}

	public static MissionLikeStatusResponse of(final List<PlanLikeItem> likes) {
		return new MissionLikeStatusResponse(likes.size(), likes);
	}
}
