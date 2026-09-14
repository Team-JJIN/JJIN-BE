package com.JJIN.domain.mission.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TravelPlanMissionAuthenticateRequest(
	@Schema(
		description = "업로드가 완료된 인증 사진의 S3 객체 key. Presigned URL 응답의 fileName 값",
		example = "mission-proof/550e8400-e29b-41d4-a716-446655440000_proof.jpg"
	)
	@NotBlank(message = "인증 사진 key는 필수입니다.")
	@Size(max = 2048, message = "인증 사진 key는 2048자 이하여야 합니다.")
	@Pattern(
		regexp = "^mission-proof/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_[^\\r\\n]+$",
		message = "인증 사진 key 형식이 올바르지 않습니다."
	)
	String proofImageKey
) {
}
