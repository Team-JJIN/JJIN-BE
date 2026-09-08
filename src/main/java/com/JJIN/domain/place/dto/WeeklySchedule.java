package com.JJIN.domain.place.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * PlaceOperatingInfo.weeklyScheduleJson의 논리 스키마
 * 요일별 운영 상태와 운영/브레이크 구간을 담는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeeklySchedule(
	List<DaySchedule> days
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record DaySchedule(
		DayOfWeek dayOfWeek,
		BusinessDayStatus status,
		List<OperatingInterval> intervals
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OperatingInterval(
		LocalTime openTime,
		LocalTime closeTime,
		LocalTime breakStartTime,
		LocalTime breakEndTime
	) {
	}
}
