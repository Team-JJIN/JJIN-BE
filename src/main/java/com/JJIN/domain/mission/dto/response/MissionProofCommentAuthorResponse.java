package com.JJIN.domain.mission.dto.response;

import com.JJIN.domain.member.entity.Member;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 인증 댓글 작성자 정보")
public record MissionProofCommentAuthorResponse(

	@Schema(description = "작성자 회원 ID", example = "2")
	Long memberId,

	@Schema(description = "작성자 닉네임", example = "서연", nullable = true)
	String nickname
) {

	public static MissionProofCommentAuthorResponse from(final Member member) {
		return new MissionProofCommentAuthorResponse(member.getId(), member.getNickname());
	}
}
