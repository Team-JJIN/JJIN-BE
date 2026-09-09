package com.JJIN.domain.place.schedule;

import java.time.LocalTime;

/**
 * 하루 안의 영업 시간대 한 구간 (열림~닫힘).
 * 종료 시각 "24:00"은 저장 편의상 23:59로 clamp된다(다음날 넘어가는 영업은 미지원).
 */
public record DayTimeRange(LocalTime open, LocalTime close) {

	public boolean contains(final LocalTime time) {
		return !time.isBefore(open) && !time.isAfter(close);
	}
}
