package com.JJIN.domain.place.schedule;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.JJIN.domain.place.entity.PlaceOperatingInfo;
import com.JJIN.domain.place.entity.enums.OpenStatus;
import com.JJIN.domain.place.entity.enums.OperatingInfoParseStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * PlaceOperatingInfo의 weeklyScheduleJson을 역직렬화해 요청 시각 기준 운영 상태를 계산한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceOpenStatusResolver {

	private final OpenStatusCalculator openStatusCalculator;
	private final ObjectMapper objectMapper;

	public OpenStatus resolve(final PlaceOperatingInfo operating, final LocalDateTime now) {
		if (operating == null
			|| operating.getParseStatus() == OperatingInfoParseStatus.NOT_PARSED
			|| operating.getParseStatus() == OperatingInfoParseStatus.FAILED
			|| operating.getWeeklyScheduleJson() == null) {
			return OpenStatus.UNKNOWN;
		}
		try {
			WeeklySchedule schedule = objectMapper.readValue(
				operating.getWeeklyScheduleJson(), WeeklySchedule.class);
			return openStatusCalculator.compute(schedule, now);
		} catch (JacksonException exception) {
			log.warn("weeklyScheduleJson 역직렬화 실패: placeId={}", operating.getPlaceId(), exception);
			return OpenStatus.UNKNOWN;
		}
	}
}
