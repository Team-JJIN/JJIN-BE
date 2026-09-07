package com.JJIN.domain.place.tourapi.client;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.tourapi.config.TourApiProperties;
import com.JJIN.domain.place.tourapi.dto.TourApiIntroItem;
import com.JJIN.domain.place.tourapi.dto.TourApiPage;
import com.JJIN.domain.place.tourapi.dto.TourApiPlaceItem;
import com.JJIN.domain.place.tourapi.exception.TourApiClientException;
import com.JJIN.domain.place.tourapi.query.AreaPlaceQuery;
import com.JJIN.domain.place.tourapi.query.FestivalQuery;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RestTourApiClient implements TourApiClient {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final RestClient restClient;
	private final TourApiProperties properties;
	private final TourApiResponseParser responseParser;

	public RestTourApiClient(
		@Qualifier("tourApiRestClient") final RestClient restClient,
		final TourApiProperties properties,
		final TourApiResponseParser responseParser
	) {
		this.restClient = restClient;
		this.properties = properties;
		this.responseParser = responseParser;
	}

	@Override
	public TourApiPage<TourApiPlaceItem> getAreaPlaces(
		final PlaceLocale locale,
		final AreaPlaceQuery query
	) {
		return get(locale, "areaBasedList2", builder -> {
			commonQuery(builder, query.page(), query.size());
			builder.queryParam("arrange", "A");
			builder.queryParam("contentTypeId", query.contentType().getContentTypeId(locale));
			queryParamIfPresent(builder, "lDongRegnCd", query.legalDongRegionCode());
			queryParamIfPresent(builder, "lDongSignguCd", query.legalDongDistrictCode());
			queryParamIfPresent(builder, "lclsSystm1", query.lclsSystm1Code());
			queryParamIfPresent(builder, "lclsSystm2", query.lclsSystm2Code());
			return builder.build();
		}, TourApiPlaceItem.class);
	}

	@Override
	public TourApiPage<TourApiPlaceItem> getFestivals(
		final PlaceLocale locale,
		final FestivalQuery query
	) {
		return get(locale, "searchFestival2", builder -> {
			commonQuery(builder, query.page(), query.size());
			builder.queryParam("arrange", "A");
			builder.queryParam("eventStartDate", BASIC_DATE.format(query.startDate()));
			builder.queryParam("eventEndDate", BASIC_DATE.format(query.endDate()));
			queryParamIfPresent(builder, "lDongRegnCd", query.legalDongRegionCode());
			return builder.build();
		}, TourApiPlaceItem.class);
	}

	@Override
	public Optional<TourApiPlaceItem> getCommonDetail(
		final PlaceLocale locale,
		final String contentId
	) {
		TourApiPage<TourApiPlaceItem> page = get(locale, "detailCommon2", builder -> {
			commonQuery(builder, 1, 1);
			builder.queryParam("contentId", contentId);
			builder.queryParam("defaultYN", "Y");
			builder.queryParam("firstImageYN", "Y");
			builder.queryParam("areacodeYN", "Y");
			builder.queryParam("catcodeYN", "Y");
			builder.queryParam("addrinfoYN", "Y");
			builder.queryParam("mapinfoYN", "Y");
			builder.queryParam("overviewYN", "Y");
			return builder.build();
		}, TourApiPlaceItem.class);
		return page.items().stream().findFirst();
	}

	@Override
	public Optional<TourApiIntroItem> getIntroDetail(
		final PlaceLocale locale,
		final String contentId,
		final TourApiContentType contentType
	) {
		TourApiPage<TourApiIntroItem> page = get(locale, "detailIntro2", builder -> {
			commonQuery(builder, 1, 1);
			builder.queryParam("contentId", contentId);
			builder.queryParam("contentTypeId", contentType.getContentTypeId(locale));
			return builder.build();
		}, TourApiIntroItem.class);
		return page.items().stream().findFirst();
	}

	private <T> TourApiPage<T> get(
		final PlaceLocale locale,
		final String operation,
		final Function<UriBuilder, URI> uriFunction,
		final Class<T> itemType
	) {
		validateServiceKey();

		try {
			JsonNode response = restClient.get()
				.uri(builder -> uriFunction.apply(builder.pathSegment(serviceName(locale), operation)))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
					throw new TourApiClientException("TourAPI HTTP 오류: " + httpResponse.getStatusCode());
				})
				.body(JsonNode.class);

			return responseParser.parse(response, itemType);
		} catch (TourApiClientException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			log.warn("TourAPI 호출 실패: locale={}, operation={}", locale, operation);
			throw new TourApiClientException("TourAPI 호출에 실패했습니다.", exception);
		}
	}

	private void commonQuery(final UriBuilder builder, final int page, final int size) {
		builder.queryParam("serviceKey", properties.serviceKey());
		builder.queryParam("MobileOS", properties.mobileOs());
		builder.queryParam("MobileApp", properties.mobileApp());
		builder.queryParam("_type", "json");
		builder.queryParam("pageNo", page);
		builder.queryParam("numOfRows", size);
	}

	private void queryParamIfPresent(
		final UriBuilder builder,
		final String name,
		final String value
	) {
		if (StringUtils.hasText(value)) {
			builder.queryParam(name, value);
		}
	}

	private String serviceName(final PlaceLocale locale) {
		return switch (locale) {
			case KO -> "KorService2";
			case EN -> "EngService2";
			case JA -> "JpnService2";
		};
	}

	private void validateServiceKey() {
		if (!StringUtils.hasText(properties.serviceKey())) {
			throw new TourApiClientException("TOUR_API_SERVICE_KEY가 설정되지 않았습니다.");
		}
	}
}
