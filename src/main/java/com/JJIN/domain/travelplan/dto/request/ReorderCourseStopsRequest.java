package com.JJIN.domain.travelplan.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 코스 방문지 순번 일괄 수정 요청.
 * 같은 일차의 모든 방문지를 (stopId, visitOrder) 쌍으로 빠짐없이 담아야 하며,
 * visitOrder는 1..N 순열이어야 한다.
 */
public record ReorderCourseStopsRequest(
	@NotEmpty(message = "orders는 비어 있을 수 없습니다.")
	@Valid
	List<StopOrder> orders
) {

	public record StopOrder(
		@NotNull(message = "stopId는 필수입니다.")
		@Positive(message = "stopId는 양수여야 합니다.")
		Long stopId,

		@NotNull(message = "visitOrder는 필수입니다.")
		@Positive(message = "visitOrder는 양수여야 합니다.")
		Integer visitOrder
	) {
	}
}
