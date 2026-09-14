package com.JJIN.global.kakao.dto;

/**
 * 경도(longitude, x), 위도(latitude, y) 좌표.
 */
public record Coordinate(
	double longitude,
	double latitude
) {

	public String toParam() {
		return longitude + "," + latitude;
	}
}
