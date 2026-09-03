package com.JJIN.global.kakao.dto;

/**
 * 두 지점 간 이동 추정 결과.
 */
public record RouteEstimate(
	int distanceMeters,
	int durationSeconds
) {

	public int durationMinutes() {
		return (int) Math.ceil(durationSeconds / 60.0);
	}
}
