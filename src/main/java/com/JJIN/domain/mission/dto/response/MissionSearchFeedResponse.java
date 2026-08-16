package com.JJIN.domain.mission.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 검색 피드 목록 응답")
public record MissionSearchFeedResponse(

	@Schema(description = "미션 카드 목록")
	List<MissionCardResponse> missions,

	@Schema(description = "검색/필터 조건에 맞는 총 미션 수", example = "53")
	long totalMissionCount,

	@Schema(description = "현재 페이지 번호. 0부터 시작", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "다음 페이지 존재 여부", example = "true")
	boolean hasNext
) {

	public static MissionSearchFeedResponse of(final Page<MissionCardResponse> page) {
		return new MissionSearchFeedResponse(
			page.getContent(),
			page.getTotalElements(),
			page.getNumber(),
			page.getSize(),
			page.hasNext()
		);
	}
}
