package com.JJIN.domain.mission.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionDifficulty {

	ONE(1),
	TWO(2),
	THREE(3),
	;

	private final int level;

	public static MissionDifficulty fromLevel(final int level) {
		for (MissionDifficulty difficulty : values()) {
			if (difficulty.level == level) {
				return difficulty;
			}
		}
		throw new IllegalArgumentException("Unsupported mission difficulty level: " + level);
	}
}
