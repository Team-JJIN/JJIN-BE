package com.JJIN.global.geo;

/**
 * 좌표 기반 직선거리 계산 유틸.
 *추천 파이프라인에서는 이 직선거리에 이동수단별 우회계수를 곱해 실거리를 근사함
 */
public final class GeoUtils {

	/** 지구 평균 반지름(km) */
	private static final double EARTH_RADIUS_KM = 6371.0088;

	private GeoUtils() {
	}

	/**
	 * 두 좌표 간 대권 직선거리를 km로 반환(Haversine).
	 */
	public static double haversineKm(final GeoPoint a, final GeoPoint b) {
		double dLat = Math.toRadians(b.latitude() - a.latitude());
		double dLon = Math.toRadians(b.longitude() - a.longitude());
		double lat1 = Math.toRadians(a.latitude());
		double lat2 = Math.toRadians(b.latitude());

		double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);

		return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(h)));
	}
}
