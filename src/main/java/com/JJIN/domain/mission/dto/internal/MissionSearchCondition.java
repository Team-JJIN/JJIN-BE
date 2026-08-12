package com.JJIN.domain.mission.dto.internal;

import java.util.List;

import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.mission.entity.enums.MissionSortOption;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;

public record MissionSearchCondition(
	String keyword,
	List<TourApiContentType> categories,
	List<MissionDifficulty> difficulties,
	MissionSortOption sortOption
) {

	public MissionSearchCondition {
		categories = categories == null ? List.of() : List.copyOf(categories);
		difficulties = difficulties == null ? List.of() : List.copyOf(difficulties);
	}
}
