package com.JJIN.global.geo;

import java.math.BigDecimal;

/**
 * 위도(latitude), 경도(longitude) 좌표
 * 카카오 요청 파라미터용 dto
 */
public record GeoPoint(
	double latitude,
	double longitude
) {

	public static GeoPoint of(final BigDecimal latitude, final BigDecimal longitude) {
		return new GeoPoint(latitude.doubleValue(), longitude.doubleValue());
	}
}
