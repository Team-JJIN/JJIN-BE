package com.JJIN.domain.place.schedule;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/**
 * 요일별 영업 시간대. weeklyScheduleJson 컬럼의 직렬화 대상.
 *
 * @param alwaysOpen "상시 개방/24시간" 케이스. true면 weekly 무시.
 * @param weekly     요일 → 시간대 목록. 값이 없는 요일은 UNKNOWN, 빈 리스트는 명시적 휴무.
 * @param notes      원문에서 뽑은 부가 문구(라스트오더, 입장마감 등). 참고용.
 */
public record WeeklySchedule(
	boolean alwaysOpen,
	Map<DayOfWeek, List<DayTimeRange>> weekly,
	List<String> notes
) {

	public static WeeklySchedule createAlwaysOpen() {
		return new WeeklySchedule(true, Map.of(), List.of());
	}
}
