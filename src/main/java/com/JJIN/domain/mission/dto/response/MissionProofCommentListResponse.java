package com.JJIN.domain.mission.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 인증 댓글 목록 응답")
public record MissionProofCommentListResponse(

	@Schema(description = "미션 인증글 ID", example = "1")
	Long proofId,

	@Schema(description = "댓글 목록")
	List<MissionProofCommentResponse> comments,

	@Schema(description = "현재 페이지 번호", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "다음 페이지 존재 여부", example = "false")
	boolean hasNext
) {

	public static MissionProofCommentListResponse of(
		final Long proofId,
		final List<MissionProofCommentResponse> comments,
		final int page,
		final int size,
		final boolean hasNext
	) {
		return new MissionProofCommentListResponse(proofId, comments, page, size, hasNext);
	}
}
