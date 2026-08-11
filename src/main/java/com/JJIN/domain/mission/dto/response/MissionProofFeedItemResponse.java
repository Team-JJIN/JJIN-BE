package com.JJIN.domain.mission.dto.response;

import java.time.LocalDateTime;

import com.JJIN.domain.mission.entity.MissionProof;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 인증 피드 항목")
public record MissionProofFeedItemResponse(

	@Schema(description = "인증글 ID", example = "1")
	Long proofId,

	@Schema(description = "작성자 정보")
	MissionProofAuthorResponse author,

	@Schema(description = "인증 이미지 URL")
	String imageUrl,

	@Schema(description = "인증 내용", example = "성수 매머드 아인슈페너 크림 진짜 미쳤음.")
	String content,

	@Schema(description = "좋아요 수", example = "42")
	int likeCount,

	@Schema(description = "댓글 수", example = "0")
	int commentCount,

	@Schema(description = "현재 사용자가 좋아요한 인증글인지 여부", example = "true")
	boolean likedByMe,

	@Schema(description = "작성 시각", example = "2026-08-11T02:09:15")
	LocalDateTime createdAt,

	@Schema(description = "연결된 미션 요약")
	MissionProofMissionSummaryResponse mission
) {

	public static MissionProofFeedItemResponse of(
		final MissionProof proof,
		final boolean likedByMe,
		final MissionProofMissionSummaryResponse mission
	) {
		return new MissionProofFeedItemResponse(
			proof.getId(),
			MissionProofAuthorResponse.from(proof.getMember()),
			proof.getImageUrl(),
			proof.getContent(),
			proof.getLikeCount(),
			proof.getCommentCount(),
			likedByMe,
			proof.getCreatedAt(),
			mission
		);
	}
}
