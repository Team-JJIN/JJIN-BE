package com.JJIN.domain.mission.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TravelPlanMissionAuthenticateRequest(
	@Schema(
		description = "업로드가 완료된 인증 사진의 공개 raw URL. PUT Presigned URL의 서명 쿼리는 제외",
		example = "https://example-bucket.s3.ap-northeast-2.amazonaws.com/mission-proof/550e8400-e29b-41d4-a716-446655440000_proof.jpg"
	)
	@NotBlank(message = "인증 사진 URL은 필수입니다.")
	@Size(max = 2048, message = "인증 사진 URL은 2048자 이하여야 합니다.")
	@Pattern(
		regexp = "^https://[^\\s/?#]+/[^\\s?#]+$",
		message = "서명 쿼리와 fragment가 없는 HTTPS 사진 URL을 전달해야 합니다."
	)
	String proofImageUrl
) {
}
