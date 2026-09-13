package com.JJIN.domain.place.schedule;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.JJIN.domain.place.entity.PlaceOperatingInfo;
import com.JJIN.domain.place.entity.enums.OpenStatus;
import com.JJIN.domain.place.entity.enums.OperatingInfoParseStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * PlaceOperatingInfo의 weeklyScheduleJson을 역직렬화해
 * 요청 시각 기준 운영 상태와 오늘의 영업 시각(열림·닫힘)을 계산한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceOpenStatusResolver {

	private static final LocalTime END_OF_DAY = LocalTime.of(23, 59);

	private final OpenStatusCalculator openStatusCalculator;
	private final ObjectMapper objectMapper;

	/**
	 * @param status    요청 시각 기준 운영 상태
	 * @param openTime  오늘의 영업 시작 시각 "HH:mm" (상시 개방은 "00:00", 휴무·미상은 null)
	 * @param closeTime 오늘의 영업 종료 시각 "HH:mm" (상시 개방은 "24:00", 휴무·미상은 null)
	 */
	public record DailyOpenInfo(OpenStatus status, String openTime, String closeTime) {

		public static DailyOpenInfo unknown() {
			return new DailyOpenInfo(OpenStatus.UNKNOWN, null, null);
		}
	}

	public DailyOpenInfo resolve(final PlaceOperatingInfo operating, final LocalDateTime now) {
		if (operating == null
			|| operating.getParseStatus() == OperatingInfoParseStatus.NOT_PARSED
			|| operating.getParseStatus() == OperatingInfoParseStatus.FAILED
			|| operating.getWeeklyScheduleJson() == null) {
			return DailyOpenInfo.unknown();
		}

		WeeklySchedule schedule;
		try {
			schedule = objectMapper.readValue(operating.getWeeklyScheduleJson(), WeeklySchedule.class);
		} catch (JacksonException exception) {
			log.warn("weeklyScheduleJson 역직렬화 실패: placeId={}", operating.getPlaceId(), exception);
			return DailyOpenInfo.unknown();
		}

		OpenStatus status = openStatusCalculator.compute(schedule, now);
		if (schedule.alwaysOpen()) {
			return new DailyOpenInfo(status, "00:00", "24:00");
		}

		List<DayTimeRange> ranges = schedule.weekly().get(now.getDayOfWeek());
		if (ranges == null || ranges.isEmpty()) {
			return new DailyOpenInfo(status, null, null);
		}

		LocalTime open = ranges.stream().map(DayTimeRange::open).min(Comparator.naturalOrder()).orElseThrow();
		LocalTime close = ranges.stream().map(DayTimeRange::close).max(Comparator.naturalOrder()).orElseThrow();
		return new DailyOpenInfo(status, formatTime(open), formatTime(close));
	}

	private String formatTime(final LocalTime time) {
		if (END_OF_DAY.equals(time)) {
			return "24:00";
		}
		return String.format("%02d:%02d", time.getHour(), time.getMinute());
	}
}
