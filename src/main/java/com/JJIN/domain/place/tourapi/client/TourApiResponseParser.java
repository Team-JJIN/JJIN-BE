package com.JJIN.domain.place.tourapi.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.JJIN.domain.place.tourapi.dto.TourApiPage;
import com.JJIN.domain.place.tourapi.exception.TourApiClientException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TourApiResponseParser {

	private static final String SUCCESS_CODE = "0000";

	private final ObjectMapper objectMapper;

	public <T> TourApiPage<T> parse(final JsonNode root, final Class<T> itemType) {
		if (root == null || root.isNull()) {
			throw new TourApiClientException("TourAPI 응답이 비어 있습니다.");
		}

		JsonNode response = root.path("response");
		JsonNode header = response.path("header");
		String resultCode = header.path("resultCode").asText();
		String resultMessage = header.path("resultMsg").asText();

		if (!SUCCESS_CODE.equals(resultCode)) {
			throw new TourApiClientException(resultCode, "TourAPI 오류: " + resultMessage);
		}

		JsonNode body = response.path("body");
		List<T> items = convertItems(body.path("items").path("item"), itemType);

		return new TourApiPage<>(
			items,
			body.path("pageNo").asInt(1),
			body.path("numOfRows").asInt(items.size()),
			body.path("totalCount").asInt(items.size())
		);
	}

	private <T> List<T> convertItems(final JsonNode itemNode, final Class<T> itemType) {
		if (itemNode.isMissingNode() || itemNode.isNull() || itemNode.isTextual()) {
			return List.of();
		}

		List<T> items = new ArrayList<>();
		if (itemNode.isArray()) {
			itemNode.forEach(node -> items.add(objectMapper.convertValue(node, itemType)));
		} else if (itemNode.isObject()) {
			items.add(objectMapper.convertValue(itemNode, itemType));
		}
		return items;
	}
}
