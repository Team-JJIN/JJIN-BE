package com.JJIN.domain.recommendation.dto;

import java.time.LocalDate;

import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.dto.WeeklySchedule;
import com.JJIN.domain.place.entity.enums.OperatingInfoParseStatus;
import com.JJIN.global.geo.GeoPoint;

/**
 * 추천 알고리즘 전용 후보 모델(파이프라인 P2·P3 입력).
 * 표시용 모델(domain/place/candidate/RecommendationCandidate)과 구분되며,
 * 운영시간·로컬도·체류시간 등 알고리즘 계산에 필요한 필드를 담는다.
 */
public record RecommendationCandidate(
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
