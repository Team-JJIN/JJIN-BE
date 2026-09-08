package com.JJIN.domain.recommendation.locality.dto;

/**
 * 시군구 단위 방문자 구성 스냅샷.
 * 관광 빅데이터 API의 touDivCd (1=현지인, 2=외지인, 3=외국인) 별 방문자수(touNum)를
 * 조회 기간 동안 누적한 값과 그로부터 산출한 현지인 비율을 담는다.
 *
 * @param districtCode        시군구 코드 (signguCode)
 * @param localVisitors       현지인(touDivCd=1) 누적 방문자수
 * @param nonLocalVisitors    외지인(touDivCd=2) 누적 방문자수
 * @param foreignVisitors     외국인(touDivCd=3) 누적 방문자수
 * @param localRatio          현지인 / 전체 방문자 비율 [0.0, 1.0] (1.0에 가까울수록 로컬)
 */
public record DistrictConcentration(
	String districtCode,
	double localVisitors,
	double nonLocalVisitors,
	double foreignVisitors,
	double localRatio
) {

	public static DistrictConcentration empty(final String districtCode) {
		return new DistrictConcentration(districtCode, 0.0, 0.0, 0.0, 0.5);
	}

	public boolean isEmpty() {
		return totalVisitors() == 0.0;
	}

	public double totalVisitors() {
		return localVisitors + nonLocalVisitors + foreignVisitors;
	}
}
