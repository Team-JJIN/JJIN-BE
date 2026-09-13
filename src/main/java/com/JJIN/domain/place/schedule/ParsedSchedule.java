package com.JJIN.domain.place.schedule;

import com.JJIN.domain.place.entity.enums.OperatingInfoParseStatus;

/**
 * WeeklyScheduleParser의 파싱 결과.
 */
public record ParsedSchedule(
	WeeklySchedule schedule,
	OperatingInfoParseStatus status
) {

	public static ParsedSchedule failed() {
		return new ParsedSchedule(null, OperatingInfoParseStatus.FAILED);
	}
}
