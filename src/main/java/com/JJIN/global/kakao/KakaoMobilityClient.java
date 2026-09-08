package com.JJIN.global.kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.JJIN.global.exception.JjinException;
import com.JJIN.global.kakao.dto.Coordinate;
import com.JJIN.global.kakao.dto.KakaoDirectionsResponse;
import com.JJIN.global.kakao.dto.RouteEstimate;
import com.JJIN.global.kakao.exception.KakaoErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 카카오모빌리티 자동차 길찾기(Directions) API 클라이언트.
 * 두 지점 간 실제 도로 기준 이동거리·소요시간을 조회한다.
 */
@Slf4j
@Component
public class KakaoMobilityClient {

	private static final String DIRECTIONS_PATH = "/v1/directions";
	private static final int RESULT_CODE_SUCCESS = 0;

	private final RestClient restClient = RestClient.create();

	@Value("${kakao.mobility.base-url}")
	private String baseUrl;

	@Value("${kakao.mobility.rest-api-key}")
	private String restApiKey;

	/**
	 * 출발지 → 목적지 자동차 경로의 이동거리(m)와 소요시간(초) 조회.
	 */
	public RouteEstimate getDrivingRoute(final Coordinate origin, final Coordinate destination) {
		String uri = UriComponentsBuilder.fromUriString(baseUrl + DIRECTIONS_PATH)
			.queryParam("origin", origin.toParam())
			.queryParam("destination", destination.toParam())
			.build()
			.toUriString();

		KakaoDirectionsResponse response = restClient.get()
			.uri(uri)
			.header(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
			.retrieve()
			.onStatus(HttpStatusCode::isError, (req, res) -> {
				log.error("카카오모빌리티 길찾기 호출 실패: status={}, origin={}, destination={}",
					res.getStatusCode(), origin.toParam(), destination.toParam());
				throw new JjinException(KakaoErrorCode.DIRECTIONS_REQUEST_FAILED);
			})
			.body(KakaoDirectionsResponse.class);

		return toEstimate(response, origin, destination);
	}

	private RouteEstimate toEstimate(
		final KakaoDirectionsResponse response,
		final Coordinate origin,
		final Coordinate destination
	) {
		KakaoDirectionsResponse.Route route = response == null ? null : response.firstRoute();
		if (route == null || route.resultCode() != RESULT_CODE_SUCCESS || route.summary() == null) {
			log.warn("카카오모빌리티 경로 탐색 실패: resultCode={}, resultMsg={}, origin={}, destination={}",
				route == null ? null : route.resultCode(),
				route == null ? null : route.resultMsg(),
				origin.toParam(), destination.toParam());
			throw new JjinException(KakaoErrorCode.ROUTE_NOT_FOUND);
		}
		return new RouteEstimate(route.summary().distance(), route.summary().duration());
	}
}
