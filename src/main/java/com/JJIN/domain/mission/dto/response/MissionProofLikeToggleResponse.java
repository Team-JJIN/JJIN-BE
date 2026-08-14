package com.JJIN.domain.mission.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 인증 좋아요 토글 응답")
public record MissionProofLikeToggleResponse(

	@Schema(description = "인증글 ID", example = "1")
	Long proofId,

	@Schema(description = "토글 후 좋아요 상태(true=좋아요)", example = "true")
	boolean liked,

	@Schema(description = "토글 후 좋아요 수", example = "43")
	int likeCount
) {

	public static MissionProofLikeToggleResponse of(
		final Long proofId,
		final boolean liked,
		final int likeCount
	) {
		return new MissionProofLikeToggleResponse(proofId, liked, likeCount);
	}
}
