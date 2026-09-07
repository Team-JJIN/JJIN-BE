package com.JJIN.domain.recommendation.service;

import org.springframework.stereotype.Component;

import com.JJIN.domain.place.dto.WeeklySchedule;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PlaceOperatingInfo.weeklyScheduleJson(문자열)을 WeeklySchedule 객체로 파싱한다.
 * 파싱 실패나 값이 없으면 null을 반환하며, 예외를 전파하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyScheduleParser {

	private final ObjectMapper objectMapper;

	public WeeklySchedule parse(final String weeklyScheduleJson) {
		if (weeklyScheduleJson == null || weeklyScheduleJson.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(weeklyScheduleJson, WeeklySchedule.class);
		} catch (Exception e) {
			log.warn("weeklyScheduleJson 파싱 실패, 운영시간 미상으로 처리합니다.", e);
			return null;
		}
	}
}
