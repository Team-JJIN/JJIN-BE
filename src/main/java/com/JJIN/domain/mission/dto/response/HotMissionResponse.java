package com.JJIN.domain.mission.dto.response;

import com.JJIN.domain.mission.entity.HotMissionItem;
import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "요즘 핫한 미션 항목")
public record HotMissionResponse(

	@Schema(description = "미션 ID", example = "10")
	Long missionId,

	@Schema(description = "미션 제목", example = "성산일출봉에서 일출 보기")
	String title,

	@Schema(description = "미션 설명", example = "새벽에 올라 일출을 인증해보세요.")
	String description,

	@Schema(description = "난이도", example = "NORMAL")
	MissionDifficulty difficulty,

	@Schema(description = "관광 타입 카테고리", example = "TOURIST_ATTRACTION", nullable = true)
	TourApiContentType category,

	@Schema(description = "대표 이미지 URL", nullable = true)
	String imageUrl,

	@Schema(description = "기간 내 미션 추가 수(집계 기준)", example = "12")
	long addedCount
) {

	public static HotMissionResponse from(final HotMissionItem item) {
		Mission mission = item.getMission();
		return new HotMissionResponse(
			mission.getId(),
			mission.getTitle(),
			mission.getDescription(),
			mission.getDifficulty(),
			mission.getCategory(),
			mission.getImageUrl(),
			item.getAddedCount()
		);
	}
}
