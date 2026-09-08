package com.JJIN.domain.recommendation.locality.client;

import java.time.LocalDate;
import java.util.Map;

import com.JJIN.domain.recommendation.locality.dto.DistrictConcentration;

/**
 * 관광 빅데이터 지역별 방문자수 API 호출 추상화.
 */
public interface ConcentrationApiClient {

	/**
	 * 지정 기간의 시군구별 방문자 스냅샷을 조회한다.
	 * API가 지역 필터를 받지 않으므로 전국 시군구가 한 번에 반환되며,
	 * 페이지네이션을 통해 모든 관측치를 (signguCode, touDivCd) 기준으로 합산한 뒤
	 * 시군구별 현지인 비율까지 계산해 돌려준다.
	 *
	 * @return signguCode → DistrictConcentration 스냅샷
	 */
	Map<String, DistrictConcentration> loadDistrictSnapshots(LocalDate startDate, LocalDate endDate);
}
