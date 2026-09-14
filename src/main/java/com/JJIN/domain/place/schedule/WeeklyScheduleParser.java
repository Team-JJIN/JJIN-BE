package com.JJIN.domain.place.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.JJIN.domain.place.entity.enums.OperatingInfoParseStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * TourAPI raw 운영시간·휴무일 텍스트를 요일별 시간대 스케줄로 변환한다.
 *
 * 관찰된 실데이터 패턴 기반 규칙 파서. LLM 미사용, 결정론적.
 * 지원 패턴:
 *  1) 단순 range: "10:00~18:00" → 전 요일 동일 range
 *  2) dash-list 요일 분기: "- 월요일~금요일 10:30~21:00 | - 토요일~일요일 10:30~20:30"
 *  3) 평일/주말 분기: "평일 09:00~21:00 | 토요일 09:00~18:00"
 *  4) 메타 문구: "상시 개방", "24시간" → alwaysOpen ; "연중무휴" → 휴무 없음
 *  5) 미상 문구: "점포별 상이", "매장별 상이", "공연별" → FAILED
 * 스킵되는 부가정보(파렌 안): "라스트오더", "마지막 주문", "입장마감", "브레이크", "준비시간".
 */
@Slf4j
@Component
public class WeeklyScheduleParser {

	private static final Pattern TIME_RANGE = Pattern.compile(
		"(\\d{1,2}):(\\d{2})\\s*[~\\-∼–—]\\s*(\\d{1,2}):(\\d{2})"
	);

	private static final Pattern PARENTHETICAL = Pattern.compile("\\([^()]*\\)");

	/** 브레이크타임 range가 키워드 앞에 오는 형태: "15:30~16:30 브레이크타임". */
	private static final Pattern BREAK_TIME_BEFORE_KW = Pattern.compile(
		"(\\d{1,2}):(\\d{2})\\s*[~\\-∼–—]\\s*(\\d{1,2}):(\\d{2})[^|()]*?(?:브레이크(?:타임)?|준비시간)"
	);
	/** 브레이크타임 range가 키워드 뒤에 오는 형태: "준비시간 15:00~16:30" / "브레이크타임 15:30~16:30". */
	private static final Pattern BREAK_KW_BEFORE_TIME = Pattern.compile(
		"(?:브레이크(?:타임)?|준비시간)[^|()]*?(\\d{1,2}):(\\d{2})\\s*[~\\-∼–—]\\s*(\\d{1,2}):(\\d{2})"
	);

	private static final List<String> ALWAYS_OPEN_KEYWORDS = List.of("상시 개방", "상시개방", "24시간");
	private static final List<String> UNKNOWN_KEYWORDS =
		List.of("점포 별로", "점포별", "매장 별로", "매장별", "공연 별", "공연별", "미상");
	private static final List<String> SKIP_SEGMENT_KEYWORDS =
		List.of("브레이크", "준비시간", "라스트오더", "마지막 주문", "입장마감", "입장 마감");
	private static final String NO_REST_KEYWORD = "연중무휴";

	public ParsedSchedule parse(final String rawOpeningHours, final String rawRestDay) {
		if (rawOpeningHours == null || rawOpeningHours.isBlank()) {
			return ParsedSchedule.failed();
		}
		// sanitize에서 파렌이 날아가기 전에 브레이크 range부터 추출.
		List<DayTimeRange> breaks = extractBreaks(rawOpeningHours);

		String text = sanitize(rawOpeningHours);
		if (text.isBlank()) {
			return ParsedSchedule.failed();
		}

		if (containsAny(text, ALWAYS_OPEN_KEYWORDS)) {
			return new ParsedSchedule(WeeklySchedule.createAlwaysOpen(), OperatingInfoParseStatus.PARSED);
		}
		if (containsAny(text, UNKNOWN_KEYWORDS) && !TIME_RANGE.matcher(text).find()) {
			return ParsedSchedule.failed();
		}

		Map<DayOfWeek, List<DayTimeRange>> weekly = new EnumMap<>(DayOfWeek.class);
		List<String> notes = new ArrayList<>();
		boolean anyMatched = extractRangesIntoWeekly(text, weekly, notes);
		if (!anyMatched && weekly.isEmpty()) {
			return ParsedSchedule.failed();
		}

		if (!breaks.isEmpty()) {
			applyBreaks(weekly, breaks);
		}
		applyRestDays(rawRestDay, weekly);

		int coveredDays = weekly.size();
		OperatingInfoParseStatus status = coveredDays == 7
			? OperatingInfoParseStatus.PARSED
			: OperatingInfoParseStatus.PARTIAL;

		return new ParsedSchedule(new WeeklySchedule(false, weekly, notes), status);
	}

	/** 원문에서 브레이크타임/준비시간 range를 뽑아 별도 리스트로 반환. */
	private List<DayTimeRange> extractBreaks(final String raw) {
		List<DayTimeRange> breaks = new ArrayList<>();
		collectRangeMatches(BREAK_TIME_BEFORE_KW.matcher(raw), breaks);
		collectRangeMatches(BREAK_KW_BEFORE_TIME.matcher(raw), breaks);
		return breaks;
	}

	private void collectRangeMatches(final Matcher m, final List<DayTimeRange> out) {
		while (m.find()) {
			try {
				LocalTime open = safeTime(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
				LocalTime close = safeTime(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
				if (open != null && close != null && open.isBefore(close)) {
					out.add(new DayTimeRange(open, close));
				}
			} catch (NumberFormatException ignore) {
			}
		}
	}

	/** 브레이크 range를 각 요일의 주 range에서 빼내어 sub-range 두 개로 쪼갠다. */
	private void applyBreaks(
		final Map<DayOfWeek, List<DayTimeRange>> weekly,
		final List<DayTimeRange> breaks
	) {
		for (Map.Entry<DayOfWeek, List<DayTimeRange>> entry : weekly.entrySet()) {
			List<DayTimeRange> ranges = entry.getValue();
			if (ranges.isEmpty()) {
				continue;
			}
			List<DayTimeRange> current = new ArrayList<>(ranges);
			for (DayTimeRange br : breaks) {
				List<DayTimeRange> next = new ArrayList<>();
				for (DayTimeRange main : current) {
					next.addAll(subtractBreak(main, br));
				}
				current = next;
			}
			ranges.clear();
			ranges.addAll(current);
		}
	}

	private List<DayTimeRange> subtractBreak(final DayTimeRange main, final DayTimeRange br) {
		if (!br.open().isBefore(main.close()) || !br.close().isAfter(main.open())) {
			return List.of(main);
		}
		List<DayTimeRange> result = new ArrayList<>();
		if (br.open().isAfter(main.open())) {
			result.add(new DayTimeRange(main.open(), br.open()));
		}
		if (br.close().isBefore(main.close())) {
			result.add(new DayTimeRange(br.close(), main.close()));
		}
		return result;
	}

	private String sanitize(final String raw) {
		if (raw == null) {
			return null;
		}
		String text = raw.replace("<br>", "|").replace("<BR>", "|").replace("\n", "|");
		text = text.replace(' ', ' ').trim();
		String previous;
		do {
			previous = text;
			text = PARENTHETICAL.matcher(text).replaceAll(" ");
		} while (!previous.equals(text));
		return text.trim();
	}

	/**
	 * 세그먼트별로 요일 세트를 감지해 first time-range를 할당한다.
	 * 요일 감지에 실패한 세그먼트에 range가 있으면 마지막 fallback으로 전 요일 동일 적용.
	 * @return 하나라도 매칭이 있었는지 여부
	 */
	private boolean extractRangesIntoWeekly(
		final String text,
		final Map<DayOfWeek, List<DayTimeRange>> weekly,
		final List<String> notes
	) {
		String[] segments = text.split("\\||/");
		boolean anyDaySpecific = false;
		DayTimeRange fallbackRange = null;

		for (String segment : segments) {
			String seg = segment.trim();
			if (seg.isEmpty()) {
				continue;
			}
			// 브레이크/준비시간/입장마감/라스트오더 세그먼트는 주 range 계산에서 제외.
			// (브레이크는 extractBreaks에서 이미 별도로 캡처됨)
			if (containsAny(seg, SKIP_SEGMENT_KEYWORDS)) {
				notes.add(seg);
				continue;
			}

			DayTimeRange range = firstRange(seg);
			if (range == null) {
				continue;
			}

			Set<DayOfWeek> days = detectDays(seg);
			if (days.isEmpty()) {
				if (fallbackRange == null) {
					fallbackRange = range;
				}
				continue;
			}
			anyDaySpecific = true;
			for (DayOfWeek day : days) {
				weekly.computeIfAbsent(day, k -> new ArrayList<>()).add(range);
			}
		}

		if (weekly.isEmpty() && fallbackRange != null) {
			for (DayOfWeek day : DayOfWeek.values()) {
				weekly.put(day, new ArrayList<>(List.of(fallbackRange)));
			}
			return true;
		}
		return anyDaySpecific;
	}

	private DayTimeRange firstRange(final String segment) {
		Matcher m = TIME_RANGE.matcher(segment);
		while (m.find()) {
			try {
				int oh = Integer.parseInt(m.group(1));
				int om = Integer.parseInt(m.group(2));
				int ch = Integer.parseInt(m.group(3));
				int cm = Integer.parseInt(m.group(4));
				LocalTime open = safeTime(oh, om);
				LocalTime close = safeTime(ch, cm);
				if (open == null || close == null) {
					continue;
				}
				return new DayTimeRange(open, close);
			} catch (NumberFormatException ignore) {
				// try next match
			}
		}
		return null;
	}

	private LocalTime safeTime(final int hour, final int minute) {
		if (hour == 24 && minute == 0) {
			return LocalTime.of(23, 59);
		}
		if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
			return null;
		}
		return LocalTime.of(hour, minute);
	}

	private Set<DayOfWeek> detectDays(final String seg) {
		Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
		String s = seg;

		if (s.contains("매일") || s.contains("365일") || s.contains("연중") || s.contains("전일")) {
			return EnumSet.allOf(DayOfWeek.class);
		}
		if (s.contains("평일") || s.contains("주중")) {
			days.addAll(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
				DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
		}
		if (s.contains("주말")) {
			days.add(DayOfWeek.SATURDAY);
			days.add(DayOfWeek.SUNDAY);
		}

		Matcher dayRange = Pattern.compile("([월화수목금토일])(?:요일)?\\s*[~\\-∼–—]\\s*([월화수목금토일])(?:요일)?")
			.matcher(s);
		while (dayRange.find()) {
			DayOfWeek start = toDayOfWeek(dayRange.group(1).charAt(0));
			DayOfWeek end = toDayOfWeek(dayRange.group(2).charAt(0));
			addRange(days, start, end);
		}

		Matcher single = Pattern.compile("([월화수목금토일])요일").matcher(s);
		while (single.find()) {
			days.add(toDayOfWeek(single.group(1).charAt(0)));
		}

		return days;
	}

	private void addRange(final Set<DayOfWeek> target, final DayOfWeek start, final DayOfWeek end) {
		int i = start.getValue();
		int last = end.getValue();
		if (last < i) {
			last += 7;
		}
		while (i <= last) {
			target.add(DayOfWeek.of(((i - 1) % 7) + 1));
			i++;
		}
	}

	private DayOfWeek toDayOfWeek(final char c) {
		return switch (c) {
			case '월' -> DayOfWeek.MONDAY;
			case '화' -> DayOfWeek.TUESDAY;
			case '수' -> DayOfWeek.WEDNESDAY;
			case '목' -> DayOfWeek.THURSDAY;
			case '금' -> DayOfWeek.FRIDAY;
			case '토' -> DayOfWeek.SATURDAY;
			case '일' -> DayOfWeek.SUNDAY;
			default -> throw new IllegalArgumentException("Unknown day: " + c);
		};
	}

	private void applyRestDays(final String rawRestDay, final Map<DayOfWeek, List<DayTimeRange>> weekly) {
		if (rawRestDay == null || rawRestDay.isBlank()) {
			return;
		}
		String rest = sanitize(rawRestDay);
		if (rest.contains(NO_REST_KEYWORD)) {
			return;
		}
		Matcher m = Pattern.compile("(?:매주\\s*)?([월화수목금토일])요일").matcher(rest);
		while (m.find()) {
			DayOfWeek closed = toDayOfWeek(m.group(1).charAt(0));
			weekly.put(closed, new ArrayList<>());
		}
	}

	private boolean containsAny(final String text, final List<String> keywords) {
		for (String k : keywords) {
			if (text.contains(k)) {
				return true;
			}
		}
		return false;
	}
}
