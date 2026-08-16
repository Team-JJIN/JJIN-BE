package com.JJIN.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.JJIN.domain.mission.entity.MissionProofComment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 인증 댓글")
public record MissionProofCommentResponse(

	@Schema(description = "댓글 ID", example = "10")
	Long commentId,

	@Schema(description = "댓글 작성자 정보")
	MissionProofCommentAuthorResponse author,

	@Schema(description = "댓글 내용", example = "여기 진짜 맛있어 보여요!")
	String content,

	@Schema(description = "댓글 작성 시각", example = "2026-08-13T14:27:00")
	LocalDateTime createdAt
) {

	public static MissionProofCommentResponse from(final MissionProofComment comment) {
		return new MissionProofCommentResponse(
			comment.getId(),
			MissionProofCommentAuthorResponse.from(comment.getMember()),
			comment.getContent(),
			comment.getCreatedAt()
		);
	}
}
