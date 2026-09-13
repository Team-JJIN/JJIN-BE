package com.JJIN.domain.place.schedule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.JJIN.domain.place.entity.enums.OperatingInfoParseStatus;
import tools.jackson.databind.ObjectMapper;

public class WeeklyScheduleParserVerification {

	@org.junit.jupiter.api.Test
	public void verifyAgainstRealSamples() throws IOException {
		Path path = Path.of("/tmp/tourapi/samples.txt");
		if (!Files.exists(path)) {
			System.out.println("샘플 파일 없음: " + path);
			return;
		}

		WeeklyScheduleParser parser = new WeeklyScheduleParser();
		Map<OperatingInfoParseStatus, Integer> counts = new EnumMap<>(OperatingInfoParseStatus.class);
		int total = 0;
		int alwaysOpen = 0;
		int failedIdx = 0;
		int successIdx = 0;

		// 두 컬럼(field\tvalue) 라인 → usetime 계열만 파싱 대상.
		List<String> lines = Files.readAllLines(path);
		Map<String, String> openingByIndex = new java.util.LinkedHashMap<>();
		Map<String, String> restByIndex = new java.util.LinkedHashMap<>();
		int idx = 0;
		String pendingOpening = null;
		for (String line : lines) {
			String[] parts = line.split("\t", 2);
			if (parts.length != 2) continue;
			String key = parts[0].toLowerCase();
			String val = parts[1];
			if (key.contains("restdate")) {
				if (pendingOpening != null) {
					String label = "sample-" + idx++;
					openingByIndex.put(label, pendingOpening);
					restByIndex.put(label, val);
					pendingOpening = null;
				}
			} else {
				if (pendingOpening != null) {
					String label = "sample-" + idx++;
					openingByIndex.put(label, pendingOpening);
					restByIndex.put(label, null);
				}
				pendingOpening = val;
			}
		}
		if (pendingOpening != null) {
			openingByIndex.put("sample-" + idx++, pendingOpening);
			restByIndex.put("sample-" + (idx - 1), null);
		}

		for (Map.Entry<String, String> entry : openingByIndex.entrySet()) {
			String label = entry.getKey();
			String opening = entry.getValue();
			String rest = restByIndex.get(label);
			ParsedSchedule parsed = parser.parse(opening, rest);
			total++;
			counts.merge(parsed.status(), 1, Integer::sum);
			if (parsed.schedule() != null && parsed.schedule().alwaysOpen()) {
				alwaysOpen++;
			}

			if (parsed.status() == OperatingInfoParseStatus.FAILED && failedIdx < 8) {
				System.out.println("[FAILED] opening=" + trunc(opening) + " | rest=" + trunc(rest));
				failedIdx++;
			}
			if ((parsed.status() == OperatingInfoParseStatus.PARSED
				|| parsed.status() == OperatingInfoParseStatus.PARTIAL) && successIdx < 10) {
				System.out.println("[" + parsed.status() + "] opening=" + trunc(opening)
					+ " | rest=" + trunc(rest)
					+ "\n         → " + summarize(parsed.schedule()));
				successIdx++;
			}
		}

		System.out.println("\n=== 요약 ===");
		System.out.println("총 샘플: " + total);
		System.out.println("alwaysOpen: " + alwaysOpen);
		counts.forEach((k, v) -> System.out.println(k + ": " + v));

		// 브레이크타임 케이스 명시 검증
		System.out.println("\n=== 브레이크타임 처리 검증 ===");
		String[][] breakCases = new String[][] {
			{"10:00~22:00 (15:30~16:30 브레이크타임)", null},
			{"- 11:00~22:00 | - 준비시간 15:00~16:30 | - 마지막 주문 21:00", "연중무휴"},
			{"- 11:20~21:30 | - 준비시간 15:00~17:00", null}
		};
		ObjectMapper omBreak = new ObjectMapper();
		for (String[] c : breakCases) {
			ParsedSchedule p = parser.parse(c[0], c[1]);
			System.out.println("input: " + c[0]);
			try {
				String j = omBreak.writeValueAsString(p.schedule());
				System.out.println("  json: " + (j.length() > 180 ? j.substring(0, 180) + "..." : j));
			} catch (Exception e) {
				System.out.println("  json error: " + e.getMessage());
			}
		}

		// JSON 직렬화 → 역직렬화 roundtrip (DB에 저장될 실제 형태 검증)
		ObjectMapper om = new ObjectMapper();
		System.out.println("\n=== weeklyScheduleJson 저장 예시 (roundtrip 검증) ===");
		int shown = 0;
		for (Map.Entry<String, String> entry : openingByIndex.entrySet()) {
			if (shown >= 4) break;
			ParsedSchedule parsed = parser.parse(entry.getValue(), restByIndex.get(entry.getKey()));
			if (parsed.schedule() == null) continue;
			try {
				String json = om.writeValueAsString(parsed.schedule());
				WeeklySchedule back = om.readValue(json, WeeklySchedule.class);
				boolean ok = back.alwaysOpen() == parsed.schedule().alwaysOpen()
					&& back.weekly().size() == parsed.schedule().weekly().size();
				System.out.println("input : " + trunc(entry.getValue()));
				System.out.println("json  : " + trunc(json));
				System.out.println("roundtrip 성공: " + ok);
				System.out.println();
				shown++;
			} catch (Exception e) {
				System.out.println("직렬화 실패: " + e.getMessage());
			}
		}
	}

	private static String trunc(final String s) {
		if (s == null) return "(null)";
		return s.length() > 100 ? s.substring(0, 100) + "..." : s;
	}

	private static String summarize(final WeeklySchedule schedule) {
		if (schedule == null) return "(null)";
		if (schedule.alwaysOpen()) return "alwaysOpen";
		StringBuilder sb = new StringBuilder();
		schedule.weekly().forEach((day, ranges) -> {
			sb.append(day.name(), 0, 3).append(":");
			if (ranges.isEmpty()) sb.append("휴무");
			else {
				for (DayTimeRange r : ranges) sb.append(r.open()).append("~").append(r.close());
			}
			sb.append("  ");
		});
		return sb.toString();
	}
}
