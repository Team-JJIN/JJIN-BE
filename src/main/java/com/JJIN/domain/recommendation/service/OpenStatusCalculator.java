package com.JJIN.domain.recommendation.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.JJIN.domain.place.dto.BusinessDayStatus;
import com.JJIN.domain.place.dto.WeeklySchedule;
import com.JJIN.domain.place.dto.WeeklySchedule.DaySchedule;
import com.JJIN.domain.place.dto.WeeklySchedule.OperatingInterval;
import com.JJIN.domain.place.entity.enums.OpenStatus;

/**
 * 검증된 주간 일정과 특정 시각으로 OpenStatus를 계산하고, 활동시간과의 운영시간 겹침을 판정한다.
 */
@Component
public class OpenStatusCalculator {

	/**
	 * 주어진 시각 기준 운영 상태를 계산한다(기획서 9장 순서).
	 */
	public OpenStatus calculate(final WeeklySchedule schedule, final LocalDateTime dateTime) {
		DaySchedule day = findDay(schedule, dateTime.getDayOfWeek());
		if (day == null || day.status() == BusinessDayStatus.UNKNOWN) {
			return OpenStatus.UNKNOWN;
		}
		if (day.status() == BusinessDayStatus.CLOSED) {
			return OpenStatus.CLOSED;
		}

		LocalTime time = dateTime.toLocalTime();
		List<OperatingInterval> intervals = day.intervals() == null ? List.of() : day.intervals();

		if (intervals.stream().anyMatch(interval -> inBreak(interval, time))) {
			return OpenStatus.BREAK;
		}
		if (intervals.stream().anyMatch(interval -> inOperating(interval, time))) {
			return OpenStatus.OPEN;
		}
		return OpenStatus.CLOSED;
	}

	/**
	 * 방문일의 운영시간이 활동 시간대와 겹치는지 판정한다(하드 필터용).
	 * 운영시간 미상(UNKNOWN)이면 제외하지 않기 위해 true를 반환한다(패널티는 스코어링에서 적용).
	 */
	public boolean overlapsActivityWindow(
		final WeeklySchedule schedule,
		final DayOfWeek visitDay,
		final LocalTime activityStart,
		final LocalTime activityEnd
	) {
		DaySchedule day = findDay(schedule, visitDay);
		if (day == null || day.status() == BusinessDayStatus.UNKNOWN) {
			return true;
		}
		if (day.status() == BusinessDayStatus.CLOSED) {
			return false;
		}
		List<OperatingInterval> intervals = day.intervals() == null ? List.of() : day.intervals();
		return intervals.stream().anyMatch(interval ->
			interval.openTime() != null && interval.closeTime() != null
				&& interval.openTime().isBefore(activityEnd)
				&& activityStart.isBefore(interval.closeTime()));
	}

	/**
	 * 방문일의 운영시간이 활동 시간대를 완전히 포함하는지 판정한다(TimeFit 만점 판정용).
	 */
	public boolean coversActivityWindow(
		final WeeklySchedule schedule,
		final DayOfWeek visitDay,
		final LocalTime activityStart,
		final LocalTime activityEnd
	) {
		DaySchedule day = findDay(schedule, visitDay);
		if (day == null || day.status() != BusinessDayStatus.OPEN) {
			return false;
		}
		List<OperatingInterval> intervals = day.intervals() == null ? List.of() : day.intervals();
		return intervals.stream().anyMatch(interval ->
			interval.openTime() != null && interval.closeTime() != null
				&& !interval.openTime().isAfter(activityStart)
				&& !interval.closeTime().isBefore(activityEnd));
	}

	private DaySchedule findDay(final WeeklySchedule schedule, final DayOfWeek dayOfWeek) {
		if (schedule == null || schedule.days() == null) {
			return null;
		}
		return schedule.days().stream()
			.filter(day -> day.dayOfWeek() == dayOfWeek)
			.findFirst()
			.orElse(null);
	}

	private boolean inBreak(final OperatingInterval interval, final LocalTime time) {
		return interval.breakStartTime() != null && interval.breakEndTime() != null
			&& !time.isBefore(interval.breakStartTime()) && time.isBefore(interval.breakEndTime());
	}

	private boolean inOperating(final OperatingInterval interval, final LocalTime time) {
		return interval.openTime() != null && interval.closeTime() != null
			&& !time.isBefore(interval.openTime()) && time.isBefore(interval.closeTime());
	}
}
