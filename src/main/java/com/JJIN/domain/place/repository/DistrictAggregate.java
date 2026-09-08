package com.JJIN.domain.place.repository;

/**
 * 시군구별 후보 집계 결과(시군구 선정용).
 * placeCount는 사용자 레벨 구간에 속하는 장소 수, avg 좌표는 시군구 대표 좌표(인접 판정용)다.
 */
public interface DistrictAggregate {

	String getDistrictCode();

	long getPlaceCount();

	double getAvgLatitude();

	double getAvgLongitude();
}
