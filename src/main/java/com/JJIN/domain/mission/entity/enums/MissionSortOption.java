package com.JJIN.domain.mission.entity.enums;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionSortOption {

	POPULAR("popular"),
	LATEST("latest"),
	;

	private final String value;

	public static MissionSortOption from(final String value) {
		if (value == null || value.isBlank()) {
			return POPULAR;
		}
		return Arrays.stream(values())
			.filter(sortOption -> sortOption.value.equalsIgnoreCase(value.trim()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unsupported mission sort option: " + value));
	}
}
