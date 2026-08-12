package com.JJIN.domain.mission.dto.response;

import java.util.List;

import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 상세 응답")
public record MissionDetailResponse(

	@Schema(description = "미션 ID", example = "1")
	Long missionId,

	@Schema(description = "미션명", example = "전통시장 한 끼 먹기")
	String title,

	@Schema(description = "대표 이미지 URL", example = "https://cdn.example.com/mission/1.jpg", nullable = true)
	String representativeImageUrl,

	@Schema(description = "미션 설명", example = "지역 전통시장에서 한 끼를 먹고 인증 사진을 남겨보세요.")
	String description,

	@Schema(description = "미션 태그 목록", example = "[\"음식\", \"전통시장\"]")
	List<String> tags,

	@Schema(description = "미션 카테고리 코드", example = "RESTAURANT")
	TourApiContentType category,

	@Schema(description = "미션 난이도 코드", example = "TWO")
	MissionDifficulty difficulty,

	@Schema(description = "현재 사용자가 이미 추가한 미션 여부", example = "true")
	boolean isAdded
) {

	public static MissionDetailResponse of(
		final Mission mission,
		final List<String> tags,
		final boolean added
	) {
		return new MissionDetailResponse(
			mission.getId(),
			mission.getTitle(),
			mission.getImageUrl(),
			mission.getDescription(),
			tags,
			mission.getCategory(),
			mission.getDifficulty(),
			added
		);
	}
}
