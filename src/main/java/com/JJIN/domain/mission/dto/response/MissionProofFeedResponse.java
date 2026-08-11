package com.JJIN.domain.mission.dto.response;

import java.util.List;

import com.JJIN.domain.mission.entity.enums.MissionProofFeedTab;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 인증 피드 응답")
public record MissionProofFeedResponse(

	@Schema(description = "조회한 탭", example = "LATEST")
	MissionProofFeedTab tab,

	@Schema(description = "페이지 번호(0부터 시작)", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "10")
	int size,

	@Schema(description = "다음 페이지 존재 여부", example = "true")
	boolean hasNext,

	@Schema(description = "인증글 목록")
	List<MissionProofFeedItemResponse> items
) {

	public static MissionProofFeedResponse of(
		final MissionProofFeedTab tab,
		final int page,
		final int size,
		final boolean hasNext,
		final List<MissionProofFeedItemResponse> items
	) {
		return new MissionProofFeedResponse(tab, page, size, hasNext, items);
	}
}
