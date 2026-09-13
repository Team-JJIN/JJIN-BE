package com.JJIN.domain.travelplan.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * 특정 여행 일정의 n일차 코스 조회 응답.
 */
public record TravelCourseDayResponse(
	Long planId,
	String planName,
	int dayNumber,
	int totalDays,
	LocalDate date,
	int stopCount,
	List<CourseStopResponse> stops
) {
}
