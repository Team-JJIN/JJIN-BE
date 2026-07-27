package com.JJIN.domain.onboarding.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 여행 경험 밀도(가볍게 / 보통 / 깊게).
 */
@Getter
@RequiredArgsConstructor
public enum ExperienceLevel {

	LIGHT("가볍게"),
	NORMAL("보통"),
	DEEP("깊게"),
	;

	private final String displayName;
}
