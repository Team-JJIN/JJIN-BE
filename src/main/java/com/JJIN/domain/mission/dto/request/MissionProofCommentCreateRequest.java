package com.JJIN.domain.mission.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 인증 댓글 작성 요청")
public record MissionProofCommentCreateRequest(

	@Schema(
		description = "댓글 내용(최대 500자)",
		example = "여기 진짜 맛있어 보여요!",
		requiredMode = Schema.RequiredMode.REQUIRED
	)
	String content
) {
}
