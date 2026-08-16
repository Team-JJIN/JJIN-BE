package com.JJIN.domain.mission.dto.request;

import java.util.List;

import com.JJIN.domain.mission.entity.enums.MissionDifficulty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMissionRequest(
	@NotBlank(message = "이미지 URL은 필수입니다.")
	String imageUrl,

	@NotBlank(message = "미션 제목은 필수입니다.")
	@Size(max = 30, message = "미션 제목은 30자 이하여야 합니다.")
	String title,

	@NotBlank(message = "미션 설명은 필수입니다.")
	@Size(max = 150, message = "미션 설명은 150자 이하여야 합니다.")
	String description,

	@NotNull(message = "미션 난이도는 필수입니다.")
	MissionDifficulty difficulty,

	List<@NotBlank(message = "빈 태그는 허용되지 않습니다.") String> tags
) {
}
