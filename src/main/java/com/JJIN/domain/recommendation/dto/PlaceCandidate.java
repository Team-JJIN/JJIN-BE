package com.JJIN.domain.recommendation.dto;

import java.time.LocalDate;

import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.dto.WeeklySchedule;
import com.JJIN.domain.place.entity.enums.OperatingInfoParseStatus;
import com.JJIN.global.geo.GeoPoint;

/**
 * 추천 알고리즘이 다루는 후보 장소 계약(파이프라인 P2·P3 입력).
 * Place와 운영정보를 조합하고, 파싱된 주간 일정·예상 체류시간을 함께 담는다.
 */
public record PlaceCandidate(
	Long placeId,
	TourApiContentType contentType,
	String lclsSystm1Code,
	String lclsSystm2Code,
	GeoPoint location,
	Double localityScore,
	ExperienceLevel localityLevel,
	OperatingInfoParseStatus parseStatus,
	WeeklySchedule weeklySchedule,
	LocalDate festivalStartDate,
	LocalDate festivalEndDate,
	int estimatedStayMinutes
) {

	public boolean isFestival() {
		return contentType == TourApiContentType.FESTIVAL_EVENT;
	}
}
