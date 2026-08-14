package com.JJIN.domain.mission.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.JJIN.domain.mission.entity.HotMissionItem;
import com.JJIN.domain.mission.entity.HotMissionSnapshot;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "요즘 핫한 미션 목록 응답")
public record HotMissionListResponse(

	@Schema(description = "집계 기간 시작(포함)", nullable = true)
	LocalDateTime windowStart,

	@Schema(description = "집계 기간 종료(제외)", nullable = true)
	LocalDateTime windowEnd,

	@Schema(description = "스냅샷 집계 시각", nullable = true)
	LocalDateTime computedAt,

	@Schema(description = "이 시각 이후 목록이 갱신된다(그 전까지 고정)", nullable = true)
	LocalDateTime nextRefreshAt,

	@Schema(description = "핫 미션 목록(추가수 많은 순)")
	List<HotMissionResponse> missions
) {

	public static HotMissionListResponse of(
		final HotMissionSnapshot snapshot,
		final List<HotMissionItem> items
	) {
		return new HotMissionListResponse(
			snapshot.getWindowStart(),
			snapshot.getWindowEnd(),
			snapshot.getComputedAt(),
			snapshot.getExpiresAt(),
			items.stream().map(HotMissionResponse::from).toList()
		);
	}

	public static HotMissionListResponse empty() {
		return new HotMissionListResponse(null, null, null, null, List.of());
	}
}
