package com.JJIN.domain.mission.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record TravelPlanMissionFeedUploadRequest(
	@Schema(
		description = "피드에 함께 올릴 인증 문구. 최대 1000자, 비워둘 수 있음",
		example = "시그니처 아인슈페너 성공! 크림 무너지기 전에 한 컷"
	)
	@Size(max = 1000, message = "인증 문구는 1000자 이하여야 합니다.")
	String content
) {
}
