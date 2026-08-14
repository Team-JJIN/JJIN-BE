package com.JJIN.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.JJIN.domain.mission.entity.MissionProofComment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 인증 댓글 작성 응답")
public record MissionProofCommentCreateResponse(

	@Schema(description = "댓글 ID", example = "10")
	Long commentId,

	@Schema(description = "미션 인증글 ID", example = "1")
	Long proofId,

	@Schema(description = "댓글 내용", example = "여기 진짜 맛있어 보여요!")
	String content,

	@Schema(description = "댓글 작성 시각", example = "2026-08-13T14:30:00")
	LocalDateTime createdAt,

	@Schema(description = "작성 후 전체 댓글 수", example = "1")
	int commentCount
) {

	public static MissionProofCommentCreateResponse of(
		final MissionProofComment comment,
		final int commentCount
	) {
		return new MissionProofCommentCreateResponse(
			comment.getId(),
			comment.getMissionProof().getId(),
			comment.getContent(),
			comment.getCreatedAt(),
			commentCount
		);
	}
}
