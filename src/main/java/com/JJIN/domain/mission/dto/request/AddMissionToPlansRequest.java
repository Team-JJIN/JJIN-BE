package com.JJIN.domain.mission.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AddMissionToPlansRequest(
	@NotEmpty(message = "미션을 추가할 일정 id 목록은 필수입니다.")
	List<@NotNull(message = "일정 id는 null일 수 없습니다.") Long> planIds
) {
}
