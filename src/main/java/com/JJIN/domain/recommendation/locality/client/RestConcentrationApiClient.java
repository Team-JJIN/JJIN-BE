package com.JJIN.domain.recommendation.locality.client;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.JJIN.domain.place.tourapi.config.TourApiProperties;
import com.JJIN.domain.recommendation.locality.dto.DistrictConcentration;
import com.JJIN.domain.recommendation.locality.exception.ConcentrationApiException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

/**
 * 한국관광공사 빅데이터 지역별 방문자수 API REST 클라이언트.
 *
 * 요청 파라미터(모두 필수): serviceKey, MobileOS, MobileApp, startYmd, endYmd (+ pageNo/numOfRows/_type)
 * 응답 item[]: baseYmd, signguCode, signguNm, daywkDivCd/Nm, touDivCd/Nm, touNum(소수 문자열)
 *   - touDivCd: 1=현지인, 2=외지인, 3=외국인
 *
 * 지역 필터가 없어 전국 시군구가 한 번에 반환된다. totalCount만큼 페이지를 순회하며
 * (signguCode, touDivCd) 별로 touNum을 누적한 뒤, 시군구별 현지인 비율까지 계산해 반환한다.
 */
@Slf4j
@Component
public class RestConcentrationApiClient implements ConcentrationApiClient {

	private static final String SUCCESS_CODE = "0000";
	private static final String DATA_LAB_SERVICE_PATH = "DataLabService";
	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private static final String TOU_DIV_LOCAL = "1";
	private static final String TOU_DIV_NON_LOCAL = "2";
	private static final String TOU_DIV_FOREIGN = "3";

	private final RestClient restClient;
	private final TourApiProperties properties;

	public RestConcentrationApiClient(
		@Qualifier("tourApiRestClient") final RestClient restClient,
		final TourApiProperties properties
	) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public Map<String, DistrictConcentration> loadDistrictSnapshots(
		final LocalDate startDate,
		final LocalDate endDate
	) {
		validateServiceKey();
		if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
			throw new ConcentrationApiException("잘못된 조회 기간: start=" + startDate + ", end=" + endDate);
		}

		TourApiProperties.VisitorStats visitorStats = properties.visitorStats();
		Map<String, Accumulator> accumulators = new HashMap<>();
		int pageNo = 1;
		int consumed = 0;

		while (pageNo <= visitorStats.maxPages()) {
			JsonNode root = fetchPage(startDate, endDate, pageNo);
			JsonNode body = validateAndExtractBody(root);
			int totalCount = body.path("totalCount").asInt(0);
			int itemsInPage = aggregate(body.path("items").path("item"), accumulators);
			consumed += itemsInPage;

			if (itemsInPage == 0 || consumed >= totalCount) {
				break;
			}
			pageNo++;
		}

		Map<String, DistrictConcentration> result = new HashMap<>(accumulators.size());
		accumulators.forEach((code, acc) -> result.put(code, acc.toSnapshot(code)));
		log.info("방문자수 API 집계 완료: districts={}, pages={}, records={}",
			result.size(), pageNo, consumed);
		return result;
	}

	private JsonNode fetchPage(final LocalDate startDate, final LocalDate endDate, final int pageNo) {
		try {
			return restClient.get()
				.uri(buildUri(startDate, endDate, pageNo))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw new ConcentrationApiException(
						"방문자수 API HTTP 오류: " + response.getStatusCode());
				})
				.body(JsonNode.class);
		} catch (ConcentrationApiException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			log.warn("방문자수 API 호출 실패: start={}, end={}, page={}",
				startDate, endDate, pageNo, exception);
			throw new ConcentrationApiException("방문자수 API 호출에 실패했습니다.", exception);
		}
	}

	private Function<UriBuilder, URI> buildUri(
		final LocalDate startDate,
		final LocalDate endDate,
		final int pageNo
	) {
		TourApiProperties.VisitorStats visitorStats = properties.visitorStats();
		return builder -> {
			builder.pathSegment(DATA_LAB_SERVICE_PATH, visitorStats.operation());
			builder.queryParam("serviceKey", properties.serviceKey());
			builder.queryParam("MobileOS", properties.mobileOs());
			builder.queryParam("MobileApp", properties.mobileApp());
			builder.queryParam("_type", "json");
			builder.queryParam("numOfRows", visitorStats.pageSize());
			builder.queryParam("pageNo", pageNo);
			builder.queryParam("startYmd", BASIC_DATE.format(startDate));
			builder.queryParam("endYmd", BASIC_DATE.format(endDate));
			return builder.build();
		};
	}

	private JsonNode validateAndExtractBody(final JsonNode root) {
		if (root == null || root.isNull()) {
			throw new ConcentrationApiException("방문자수 API 응답이 비어 있습니다.");
		}
		JsonNode header = root.path("response").path("header");
		String resultCode = header.path("resultCode").asText();
		if (!SUCCESS_CODE.equals(resultCode)) {
			throw new ConcentrationApiException(resultCode,
				"방문자수 API 오류: " + header.path("resultMsg").asText());
		}
		return root.path("response").path("body");
	}

	private int aggregate(final JsonNode itemNode, final Map<String, Accumulator> accumulators) {
		if (itemNode.isMissingNode() || itemNode.isNull()) {
			return 0;
		}

		int count = 0;
		if (itemNode.isArray()) {
			for (JsonNode node : itemNode) {
				if (accumulate(node, accumulators)) {
					count++;
				}
			}
		} else if (itemNode.isObject()) {
			if (accumulate(itemNode, accumulators)) {
				count = 1;
			}
		}
		return count;
	}

	private boolean accumulate(final JsonNode node, final Map<String, Accumulator> accumulators) {
		String signguCode = node.path("signguCode").asText();
		if (!StringUtils.hasText(signguCode)) {
			return false;
		}
		double touNum = parseTouNum(node.path("touNum"));
		if (touNum <= 0.0) {
			return false;
		}
		String touDivCd = node.path("touDivCd").asText();
		accumulators.computeIfAbsent(signguCode, k -> new Accumulator()).add(touDivCd, touNum);
		return true;
	}

	/** touNum은 "154108.5", "21991.27"처럼 소수 문자열로 옴. Long 파싱은 불가. */
	private double parseTouNum(final JsonNode value) {
		if (value.isMissingNode() || value.isNull()) {
			return 0.0;
		}
		String text = value.asText();
		if (!StringUtils.hasText(text)) {
			return 0.0;
		}
		try {
			return Double.parseDouble(text.trim());
		} catch (NumberFormatException exception) {
			return 0.0;
		}
	}

	private void validateServiceKey() {
		if (!StringUtils.hasText(properties.serviceKey())) {
			throw new ConcentrationApiException("TOUR_API_SERVICE_KEY가 설정되지 않았습니다.");
		}
	}

	private static final class Accumulator {
		private double localVisitors;
		private double nonLocalVisitors;
		private double foreignVisitors;

		void add(final String touDivCd, final double value) {
			switch (touDivCd) {
				case TOU_DIV_LOCAL -> localVisitors += value;
				case TOU_DIV_NON_LOCAL -> nonLocalVisitors += value;
				case TOU_DIV_FOREIGN -> foreignVisitors += value;
				default -> {
					/* 알 수 없는 구분 코드는 무시 */
				}
			}
		}

		DistrictConcentration toSnapshot(final String districtCode) {
			double total = localVisitors + nonLocalVisitors + foreignVisitors;
			double ratio = total == 0.0 ? 0.5 : localVisitors / total;
			return new DistrictConcentration(
				districtCode, localVisitors, nonLocalVisitors, foreignVisitors, ratio);
		}
	}
}
