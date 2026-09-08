package com.JJIN.domain.recommendation.locality.client;

import java.net.URI;
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
import com.JJIN.domain.recommendation.locality.exception.ConcentrationApiException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

/**
 * KTO 관광지 집중률 API REST 클라이언트.
 *
 * 요청 파라미터: serviceKey, MobileOS, MobileApp, areaCd, signguCd, pageNo, numOfRows, _type
 * 응답 item[]: baseYmd, areaCd/Nm, signguCd/Nm, tAtsNm, cnctrRate(0~100)
 * (tAtsNm 미지정 시 시군구 내 관광지 전체가 반환됨)
 *
 * 클라이언트는 페이지네이션으로 (관광지, 일자) 관측치를 모두 모아 tAtsNm 단위로 cnctrRate
 * 평균을 낸 뒤 0.0~1.0으로 정규화해 반환한다.
 */
@Slf4j
@Component
public class RestAttractionConcentrationApiClient implements AttractionConcentrationApiClient {

	private static final String SUCCESS_CODE = "0000";
	private static final double PERCENT_TO_RATIO = 100.0;

	private final RestClient restClient;
	private final TourApiProperties properties;

	public RestAttractionConcentrationApiClient(
		@Qualifier("tourApiRestClient") final RestClient restClient,
		final TourApiProperties properties
	) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public Map<String, Double> loadAttractionConcentrations(
		final String regionCode,
		final String districtCode
	) {
		validateServiceKey();
		if (!StringUtils.hasText(regionCode) || !StringUtils.hasText(districtCode)) {
			return Map.of();
		}

		TourApiProperties.AttractionConcentration config = properties.attractionConcentration();
		Map<String, double[]> sumAndCount = new HashMap<>();
		int pageNo = 1;
		int consumed = 0;

		while (pageNo <= config.maxPages()) {
			JsonNode root = fetchPage(regionCode, districtCode, pageNo);
			JsonNode body = validateAndExtractBody(root);
			int totalCount = body.path("totalCount").asInt(0);
			int itemsInPage = aggregate(body.path("items").path("item"), sumAndCount);
			consumed += itemsInPage;

			if (itemsInPage == 0 || consumed >= totalCount) {
				break;
			}
			pageNo++;
		}

		Map<String, Double> result = new HashMap<>(sumAndCount.size());
		sumAndCount.forEach((tAtsNm, agg) -> {
			double avgPercent = agg[0] / agg[1];
			result.put(tAtsNm, clamp01(avgPercent / PERCENT_TO_RATIO));
		});
		log.debug("관광지 집중률 집계: region={}, district={}, attractions={}, records={}",
			regionCode, districtCode, result.size(), consumed);
		return result;
	}

	private JsonNode fetchPage(
		final String regionCode,
		final String districtCode,
		final int pageNo
	) {
		try {
			return restClient.get()
				.uri(buildUri(regionCode, districtCode, pageNo))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw new ConcentrationApiException(
						"관광지 집중률 API HTTP 오류: " + response.getStatusCode());
				})
				.body(JsonNode.class);
		} catch (ConcentrationApiException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			log.warn("관광지 집중률 API 호출 실패: region={}, district={}, page={}",
				regionCode, districtCode, pageNo, exception);
			throw new ConcentrationApiException("관광지 집중률 API 호출에 실패했습니다.", exception);
		}
	}

	private Function<UriBuilder, URI> buildUri(
		final String regionCode,
		final String districtCode,
		final int pageNo
	) {
		TourApiProperties.AttractionConcentration config = properties.attractionConcentration();
		return builder -> {
			builder.pathSegment(config.servicePath(), config.operation());
			builder.queryParam("serviceKey", properties.serviceKey());
			builder.queryParam("MobileOS", properties.mobileOs());
			builder.queryParam("MobileApp", properties.mobileApp());
			builder.queryParam("_type", "json");
			builder.queryParam("numOfRows", config.pageSize());
			builder.queryParam("pageNo", pageNo);
			builder.queryParam("areaCd", regionCode);
			builder.queryParam("signguCd", districtCode);
			return builder.build();
		};
	}

	private JsonNode validateAndExtractBody(final JsonNode root) {
		if (root == null || root.isNull()) {
			throw new ConcentrationApiException("관광지 집중률 API 응답이 비어 있습니다.");
		}
		JsonNode header = root.path("response").path("header");
		String resultCode = header.path("resultCode").asText();
		if (!SUCCESS_CODE.equals(resultCode)) {
			throw new ConcentrationApiException(resultCode,
				"관광지 집중률 API 오류: " + header.path("resultMsg").asText());
		}
		return root.path("response").path("body");
	}

	private int aggregate(final JsonNode itemNode, final Map<String, double[]> sumAndCount) {
		if (itemNode.isMissingNode() || itemNode.isNull()) {
			return 0;
		}

		int count = 0;
		if (itemNode.isArray()) {
			for (JsonNode node : itemNode) {
				if (accumulate(node, sumAndCount)) {
					count++;
				}
			}
		} else if (itemNode.isObject()) {
			if (accumulate(itemNode, sumAndCount)) {
				count = 1;
			}
		}
		return count;
	}

	private boolean accumulate(final JsonNode node, final Map<String, double[]> sumAndCount) {
		String tAtsNm = node.path("tAtsNm").asText();
		if (!StringUtils.hasText(tAtsNm)) {
			return false;
		}
		Double rate = readCnctrRate(node);
		if (rate == null) {
			return false;
		}
		double[] agg = sumAndCount.computeIfAbsent(tAtsNm, k -> new double[2]);
		agg[0] += rate;
		agg[1] += 1;
		return true;
	}

	private Double readCnctrRate(final JsonNode node) {
		JsonNode value = node.path("cnctrRate");
		if (value.isMissingNode() || value.isNull()) {
			return null;
		}
		String text = value.asText();
		if (!StringUtils.hasText(text)) {
			return null;
		}
		try {
			return Double.parseDouble(text.trim());
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private double clamp01(final double value) {
		if (value < 0.0) {
			return 0.0;
		}
		if (value > 1.0) {
			return 1.0;
		}
		return value;
	}

	private void validateServiceKey() {
		if (!StringUtils.hasText(properties.serviceKey())) {
			throw new ConcentrationApiException("TOUR_API_SERVICE_KEY가 설정되지 않았습니다.");
		}
	}
}
