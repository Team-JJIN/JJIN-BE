package com.JJIN.domain.mission.dto.response;

import com.JJIN.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 인증글 작성자 정보")
public record MissionProofAuthorResponse(

	@Schema(description = "작성자 회원 ID", example = "1")
	Long memberId,

	@Schema(description = "작성자 닉네임", nullable = true)
	String nickname,

	@Schema(description = "작성자 프로필 이미지 URL", nullable = true)
	String profileImageUrl
) {

	public static MissionProofAuthorResponse from(final Member member) {
		return new MissionProofAuthorResponse(
			member.getId(),
			member.getNickname(),
			//TODO: 프로필 이미지 추가시 코드 변경
			null
		);
	}
}
