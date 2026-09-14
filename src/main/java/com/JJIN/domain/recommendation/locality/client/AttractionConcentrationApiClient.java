package com.JJIN.domain.recommendation.locality.client;

import java.util.Map;

/**
 * KTO 관광지 집중률 API 호출 추상화.
 */
public interface AttractionConcentrationApiClient {

	/**
	 * 시도·시군구 단위로 등록된 관광지별 평균 집중률(0.0~1.0 정규화)을 조회한다.
	 *
	 * @param regionCode   areaCd
	 * @param districtCode signguCd
	 * @return 관광지명(tAtsNm) → 평균 집중률 비율(0.0~1.0)
	 */
	Map<String, Double> loadAttractionConcentrations(String regionCode, String districtCode);
}
