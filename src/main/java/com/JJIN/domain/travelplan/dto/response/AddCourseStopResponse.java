package com.JJIN.domain.travelplan.dto.response;

/**
 * 코스 방문지 추가 응답.
 */
public record AddCourseStopResponse(
	Long stopId,
	int dayNumber,
	int visitOrder
) {

	public static AddCourseStopResponse of(final Long stopId, final int dayNumber, final int visitOrder) {
		return new AddCourseStopResponse(stopId, dayNumber, visitOrder);
	}
}
