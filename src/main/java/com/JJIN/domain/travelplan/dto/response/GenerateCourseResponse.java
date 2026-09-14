package com.JJIN.domain.travelplan.dto.response;

import java.util.List;

/**
 * 여행 코스 자동 생성(추천 파이프라인 실행) 결과 요약.
 * 상세 방문지 목록은 일자별 코스 조회 API로 확인한다.
 */
public record GenerateCourseResponse(
	Long planId,
	int totalDays,
	int totalStops,
	List<DaySummary> days
) {

	public record DaySummary(
		int dayNumber,
		int stopCount
	) {
	}
}
