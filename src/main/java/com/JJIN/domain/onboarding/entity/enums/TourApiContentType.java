package com.JJIN.domain.onboarding.entity.enums;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TourAPI KorService2 관광타입(contentTypeId).
 */
@Getter
@RequiredArgsConstructor
public enum TourApiContentType {

	TOURIST_ATTRACTION(12, "관광지", true),
	CULTURAL_FACILITY(14, "문화시설", true),
	FESTIVAL_EVENT(15, "축제/공연/행사", true),
	TRAVEL_COURSE(25, "여행코스", false),
	LEISURE_SPORTS(28, "레포츠", true),
	LODGING(32, "숙박", false),
	SHOPPING(38, "쇼핑", true),
	RESTAURANT(39, "음식점", true),
	;

	private final int contentTypeId;
	private final String displayName;
	private final boolean preferenceSelectable;

	public static Optional<TourApiContentType> fromContentTypeId(final int contentTypeId) {
		return Arrays.stream(values())
			.filter(contentType -> contentType.contentTypeId == contentTypeId)
			.findFirst();
	}
}
