package com.JJIN.domain.onboarding.entity.enums;

import static com.JJIN.domain.onboarding.entity.enums.TourApiClassification.of;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Getter;

/**
 * 온보딩 UX 세부 취향.
 * 각 값은 TourAPI KorService2 관광타입(contentTypeId)과 신분류체계(lclsSystm) 검색 조건을 가진다.
 */
@Getter
public enum TravelSubcategory {

	/* FOOD */
	KOREAN_FOOD(TourApiContentType.RESTAURANT, "한식", of("FD", "FD01")),
	CAFE_TEAHOUSE(TourApiContentType.RESTAURANT, "카페/찻집", of("FD", "FD05")),
	PUB(TourApiContentType.RESTAURANT, "술집", of("FD", "FD04")),
	LIKE_ALL_FOOD(TourApiContentType.RESTAURANT, "다 좋아요", of("FD")),

	/* EXPERIENCE */
	TRADITIONAL_EXPERIENCE(TourApiContentType.TOURIST_ATTRACTION, "전통 체험", of("EX", "EX01")),
	TEMPLE_STAY(TourApiContentType.TOURIST_ATTRACTION, "템플스테이", of("EX", "EX04", "EX040100")),
	UNIQUE_EXPERIENCE(TourApiContentType.TOURIST_ATTRACTION, "이색 체험", of("EX", "EX07")),

	/* NATURE */
	MOUNTAIN_FOREST(
		TourApiContentType.TOURIST_ATTRACTION,
		"산/숲",
		of("NA", "NA01", "NA010100"),
		of("NA", "NA01", "NA010200")
	),
	SEA_BEACH(
		TourApiContentType.TOURIST_ATTRACTION,
		"바다/해변",
		of("NA", "NA02", "NA020800"),
		of("NA", "NA02", "NA020900")
	),
	LAKE_RIVER(
		TourApiContentType.TOURIST_ATTRACTION,
		"호수/강",
		of("NA", "NA02", "NA020100"),
		of("NA", "NA02", "NA020200")
	),
	ISLAND(TourApiContentType.TOURIST_ATTRACTION, "섬", of("NA", "NA02", "NA020500")),
	PARK(
		TourApiContentType.TOURIST_ATTRACTION,
		"공원",
		of("NA", "NA04"),
		of("VE", "VE03")
	),

	/* HISTORY */
	PALACE(TourApiContentType.TOURIST_ATTRACTION, "궁궐", of("HS", "HS01", "HS010100")),
	HISTORIC_SITE(TourApiContentType.TOURIST_ATTRACTION, "유적지", of("HS", "HS01")),
	MUSEUM(TourApiContentType.CULTURAL_FACILITY, "박물관", of("VE", "VE07", "VE070100")),
	TRADITIONAL_VILLAGE(
		TourApiContentType.TOURIST_ATTRACTION,
		"전통 마을",
		of("HS", "HS01", "HS010600"),
		of("VE", "VE04", "VE040200")
	),

	/* CULTURE */
	GALLERY(TourApiContentType.CULTURAL_FACILITY, "미술관", of("VE", "VE07", "VE070600")),
	PERFORMANCE_MUSICAL(TourApiContentType.FESTIVAL_EVENT, "공연/뮤지컬", of("EV", "EV02", "EV020300")),
	STREET_ART(TourApiContentType.TOURIST_ATTRACTION, "거리 예술", of("VE", "VE04", "VE040100")),
	TEMPLE(TourApiContentType.TOURIST_ATTRACTION, "사찰", of("HS", "HS03", "HS030100")),

	/* SHOPPING */
	TRADITIONAL_MARKET(TourApiContentType.SHOPPING, "전통 시장", of("SH", "SH06")),
	LOCAL_SHOP(TourApiContentType.SHOPPING, "로컬 상점", of("SH", "SH05")),
	DUTY_FREE(TourApiContentType.SHOPPING, "면세점", of("SH", "SH04")),
	// TODO: 정의서에 빈티지와 1:1로 대응되는 lclsSystm 코드가 없어 contentType만 보관한다.
	VINTAGE(TourApiContentType.SHOPPING, "빈티지"),

	/* FESTIVAL */
	FESTIVAL_EVENT(TourApiContentType.FESTIVAL_EVENT, "축제", of("EV", "EV01")),
	PERFORMANCE_EVENT(
		TourApiContentType.FESTIVAL_EVENT,
		"공연 행사",
		of("EV", "EV02"),
		of("EV", "EV03")
	),
	// TODO: 정의서에 불꽃 축제와 1:1로 대응되는 lclsSystm 코드가 없어 contentType만 보관한다.
	FIREWORKS(TourApiContentType.FESTIVAL_EVENT, "불꽃 축제"),
	// TODO: 야시장은 행사/쇼핑 후보가 모두 가능하지만 정의서에 1:1 코드가 없어 기존 UX 흐름의 행사 타입만 보관한다.
	NIGHT_MARKET(TourApiContentType.FESTIVAL_EVENT, "야시장"),

	/* LEISURE */
	SURFING(TourApiContentType.LEISURE_SPORTS, "서핑", of("LS", "LS02", "LS020100")),
	SKIING(TourApiContentType.LEISURE_SPORTS, "스키", of("LS", "LS01", "LS010800")),
	// TODO: 등산은 레포츠/자연관광 후보가 모두 가능하지만 정의서에 1:1 코드가 없어 contentType만 보관한다.
	HIKING(TourApiContentType.LEISURE_SPORTS, "등산"),
	CYCLING(TourApiContentType.LEISURE_SPORTS, "자전거", of("LS", "LS01", "LS010200")),
	WATER_SPORTS(TourApiContentType.LEISURE_SPORTS, "수상 스포츠", of("LS", "LS02")),
	;

	private final TourApiContentType contentType;
	private final String displayName;
	private final Set<TourApiClassification> classifications;

	TravelSubcategory(
		final TourApiContentType contentType,
		final String displayName,
		final TourApiClassification... classifications
	) {
		this.contentType = contentType;
		this.displayName = displayName;
		this.classifications = Collections.unmodifiableSet(
			new LinkedHashSet<>(Arrays.asList(classifications))
		);
	}

	public boolean belongsTo(final TourApiContentType contentType) {
		return this.contentType == contentType;
	}

	public int getContentTypeId() {
		return contentType.getContentTypeId();
	}

	public static Set<TourApiContentType> toContentTypes(final Collection<TravelSubcategory> subcategories) {
		LinkedHashSet<TourApiContentType> contentTypes = new LinkedHashSet<>();
		for (TravelSubcategory subcategory : subcategories) {
			if (subcategory != null) {
				contentTypes.add(subcategory.getContentType());
			}
		}
		return Collections.unmodifiableSet(contentTypes);
	}

	public static Set<Integer> toContentTypeIds(final Collection<TravelSubcategory> subcategories) {
		LinkedHashSet<Integer> contentTypeIds = new LinkedHashSet<>();
		for (TravelSubcategory subcategory : subcategories) {
			if (subcategory != null) {
				contentTypeIds.add(subcategory.getContentTypeId());
			}
		}
		return Collections.unmodifiableSet(contentTypeIds);
	}

	public static Set<TourApiClassification> toClassifications(final Collection<TravelSubcategory> subcategories) {
		LinkedHashSet<TourApiClassification> classifications = new LinkedHashSet<>();
		for (TravelSubcategory subcategory : subcategories) {
			if (subcategory != null) {
				classifications.addAll(subcategory.getClassifications());
			}
		}
		return Collections.unmodifiableSet(classifications);
	}
}
