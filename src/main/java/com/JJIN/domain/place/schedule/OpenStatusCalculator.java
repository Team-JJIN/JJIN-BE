package com.JJIN.domain.place.schedule;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.JJIN.domain.place.entity.enums.OpenStatus;

/**
 * 요청 시각을 기준으로 WeeklySchedule에서 현재 운영 상태를 계산하고,
 * 활동 시간대와 운영시간의 겹침·포함을 판정한다.
 */
@Component
public class OpenStatusCalculator {

	public OpenStatus compute(final WeeklySchedule schedule, final LocalDateTime now) {
		if (schedule == null) {
			return OpenStatus.UNKNOWN;
		}
		if (schedule.alwaysOpen()) {
			return OpenStatus.OPEN;
		}
		List<DayTimeRange> ranges = schedule.weekly().get(now.getDayOfWeek());
		if (ranges == null) {
			return OpenStatus.UNKNOWN;
		}
		if (ranges.isEmpty()) {
			return OpenStatus.CLOSED;
		}
		for (DayTimeRange range : ranges) {
			if (range.contains(now.toLocalTime())) {
				return OpenStatus.OPEN;
			}
		}
		if (isBetweenRanges(ranges, now.toLocalTime())) {
			return OpenStatus.BREAK;
		}
		return OpenStatus.CLOSED;
	}

	/**
	 * 방문일의 운영시간이 활동 시간대와 겹치는지 판정한다(하드 필터용).
	 * 운영시간 미상(해당 요일 데이터 없음)이면 제외하지 않기 위해 true를 반환한다.
	 */
	public boolean overlapsActivityWindow(
		final WeeklySchedule schedule,
		final DayOfWeek visitDay,
		final LocalTime activityStart,
		final LocalTime activityEnd
	) {
		if (schedule == null) {
			return true;
		}
		if (schedule.alwaysOpen()) {
			return true;
		}
		List<DayTimeRange> ranges = schedule.weekly().get(visitDay);
		if (ranges == null) {
			return true;
		}
		if (ranges.isEmpty()) {
			return false;
		}
		return ranges.stream().anyMatch(range ->
			range.open().isBefore(activityEnd) && activityStart.isBefore(range.close()));
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
		if (schedule == null) {
			return false;
		}
		if (schedule.alwaysOpen()) {
			return true;
		}
		List<DayTimeRange> ranges = schedule.weekly().get(visitDay);
		if (ranges == null || ranges.isEmpty()) {
			return false;
		}
		return ranges.stream().anyMatch(range ->
			!range.open().isAfter(activityStart) && !range.close().isBefore(activityEnd));
	}

	private boolean isBetweenRanges(final List<DayTimeRange> ranges, final java.time.LocalTime now) {
		if (ranges.size() < 2) {
			return false;
		}
		java.time.LocalTime earliestOpen = ranges.get(0).open();
		java.time.LocalTime latestClose = ranges.get(0).close();
		for (DayTimeRange r : ranges) {
			if (r.open().isBefore(earliestOpen)) {
				earliestOpen = r.open();
			}
			if (r.close().isAfter(latestClose)) {
				latestClose = r.close();
			}
		}
		return !now.isBefore(earliestOpen) && !now.isAfter(latestClose);
	}
}
