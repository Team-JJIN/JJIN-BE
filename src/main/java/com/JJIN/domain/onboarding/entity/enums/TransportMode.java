package com.JJIN.domain.onboarding.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 여행 중 주 이동 수단. 온보딩에서 정확히 하나만 선택한다.
 */
@Getter
@RequiredArgsConstructor
public enum TransportMode {

	WALKING("도보"),
	PUBLIC_TRANSIT("대중교통"),
	CAR("자동차"),
	;

	private final String displayName;
}
