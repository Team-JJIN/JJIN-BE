package com.JJIN.domain.onboarding.entity.enums;

import java.util.Arrays;
import java.util.Optional;

import com.JJIN.domain.place.entity.enums.PlaceLocale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TourAPI 언어별 관광타입(contentTypeId).
 */
@Getter
@RequiredArgsConstructor
public enum TourApiContentType {

	TOURIST_ATTRACTION(12, 76, 76, "관광지", true),
	CULTURAL_FACILITY(14, 78, 78, "문화시설", true),
	FESTIVAL_EVENT(15, 85, 85, "축제/공연/행사", true),
	TRAVEL_COURSE(25, 75, 75, "여행코스", false),
	LEISURE_SPORTS(28, 77, 77, "레포츠", true),
	LODGING(32, 80, 80, "숙박", false),
	SHOPPING(38, 79, 79, "쇼핑", true),
	RESTAURANT(39, 82, 82, "음식점", true),
	;

	/** 국문 ID. 기존 호출부와의 호환을 위해 필드명을 유지한다. */
	private final int contentTypeId;
	private final int englishContentTypeId;
	private final int japaneseContentTypeId;
	private final String displayName;
	private final boolean preferenceSelectable;

	public static Optional<TourApiContentType> fromContentTypeId(final int contentTypeId) {
		return fromContentTypeId(PlaceLocale.KO, contentTypeId);
	}

	public static Optional<TourApiContentType> fromContentTypeId(
		final PlaceLocale locale,
		final int contentTypeId
	) {
		return Arrays.stream(values())
			.filter(contentType -> contentType.getContentTypeId(locale) == contentTypeId)
			.findFirst();
	}

	public int getContentTypeId(final PlaceLocale locale) {
		return switch (locale) {
			case KO -> contentTypeId;
			case EN -> englishContentTypeId;
			case JA -> japaneseContentTypeId;
		};
	}
}
