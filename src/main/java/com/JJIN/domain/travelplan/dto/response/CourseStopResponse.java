package com.JJIN.domain.travelplan.dto.response;

import java.math.BigDecimal;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.enums.OpenStatus;

/**
 * 코스 한 방문지 응답. 화면 카드 렌더에 필요한 표시 정보를 담는다.
 * @param distanceFromPreviousMeters 이전 방문지와의 직선거리(m). 첫 방문지는 null.
 */
public record CourseStopResponse(
	Long stopId,
	int visitOrder,
	Long placeId,
	String name,
	TourApiContentType category,
	String address,
	BigDecimal latitude,
	BigDecimal longitude,
	String openingHoursText,
	OpenStatus openStatus,
	Integer distanceFromPreviousMeters
) {
}
