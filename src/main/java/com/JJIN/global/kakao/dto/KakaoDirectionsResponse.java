package com.JJIN.global.kakao.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오모빌리티 자동차 길찾기(directions) 응답.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoDirectionsResponse(
	@JsonProperty("trans_id") String transId,
	List<Route> routes
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Route(
		@JsonProperty("result_code") int resultCode,
		@JsonProperty("result_msg") String resultMsg,
		Summary summary
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Summary(
		int distance, // 미터
		int duration  // 초
	) {
	}

	public Route firstRoute() {
		if (routes == null || routes.isEmpty()) {
			return null;
		}
		return routes.get(0);
	}
}
