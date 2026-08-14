package com.JJIN.domain.mission.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 검색 피드 카드 응답")
public record MissionCardResponse(

	@Schema(description = "미션 ID", example = "1")
	Long missionId,

	@Schema(description = "미션명", example = "전통시장 한 끼 먹기")
	String title,

	@Schema(description = "카드 썸네일 이미지 URL", example = "https://cdn.example.com/mission/1.jpg", nullable = true)
	String thumbnailImageUrl,

	@Schema(description = "미션 태그 목록", example = "[\"음식\", \"전통시장\"]")
	List<String> tags,

	@Schema(description = "미션 카테고리 코드", example = "RESTAURANT")
	TourApiContentType category,

	@Schema(description = "미션 난이도 코드", example = "TWO")
	MissionDifficulty difficulty,

	@Schema(description = "인기도 산정용 값. 현재는 미션을 추가한 사용자 수", example = "12")
	long popularity,

	@Schema(description = "미션 생성 시각", example = "2026-08-12T10:30:00")
	LocalDateTime createdAt,

	@Schema(description = "현재 사용자가 이미 추가한 미션 여부", example = "true")
	boolean isAdded
) {

	public static MissionCardResponse of(
		final Mission mission,
		final List<String> tags,
		final long popularity,
		final boolean added
	) {
		return new MissionCardResponse(
			mission.getId(),
			mission.getTitle(),
			mission.getImageUrl(),
			tags,
			mission.getCategory(),
			mission.getDifficulty(),
			popularity,
			mission.getCreatedAt(),
			added
		);
	}
}
