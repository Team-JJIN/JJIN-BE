package com.JJIN.domain.place.search;

import java.math.BigDecimal;
import java.util.List;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.enums.OpenStatus;

/**
 * 장소 키워드 검색 응답.
 * 검색 결과는 이미 DB에 동기화된 상태라 placeId를 그대로 코스 추가 API에 사용할 수 있다.
 *
 * @param places 사용자 좌표가 주어지면 가까운 순으로 정렬된다.
 */
public record PlaceSearchResponse(
	int totalCount,
	int page,
	int size,
	List<SearchedPlace> places
) {

	/**
	 * @param openTime       오늘의 영업 시작 시각 "HH:mm" (휴무·미상은 null)
	 * @param closeTime      오늘의 영업 종료 시각 "HH:mm" (휴무·미상은 null)
	 * @param openStatus     요청 시각 기준 운영 상태
	 * @param distanceMeters 사용자 좌표로부터의 직선거리(m). 좌표 미제공 시 null.
	 * @param alreadyAdded   요청 planId의 코스에 이미 포함된 장소인지. planId 미제공 시 항상 false.
	 */
	public record SearchedPlace(
		Long placeId,
		TourApiContentType category,
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		String representativeImageUrl,
		String openTime,
		String closeTime,
		OpenStatus openStatus,
		Integer distanceMeters,
		boolean alreadyAdded
	) {
	}
}
