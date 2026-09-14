package com.JJIN.domain.travelplan.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 여행 코스 방문지 추가 요청.
 */
public record AddCourseStopRequest(
	@NotNull(message = "placeId는 필수입니다.")
	@Positive(message = "placeId는 양수여야 합니다.")
	Long placeId,

	@NotNull(message = "dayNumber는 필수입니다.")
	@Positive(message = "dayNumber는 양수여야 합니다.")
	Integer dayNumber
) {
}
