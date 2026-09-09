package com.JJIN.domain.place.schedule;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.JJIN.domain.place.entity.enums.OpenStatus;

/**
 * 요청 시각을 기준으로 WeeklySchedule에서 현재 운영 상태를 계산한다.
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
