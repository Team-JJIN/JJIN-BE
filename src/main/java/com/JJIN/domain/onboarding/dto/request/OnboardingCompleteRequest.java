package com.JJIN.domain.onboarding.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.onboarding.entity.enums.TransportMode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "온보딩 여행 기본 정보 저장 요청")
public record OnboardingCompleteRequest(

	@Schema(
		description = "여행 지역 ID. regionUndecided가 true면 반드시 null이어야 한다.",
		example = "1",
		nullable = true
	)
	Long regionId,

	@Schema(description = "지역 미정 여부", example = "false")
	@NotNull(message = "지역 미정 여부는 필수입니다.")
	Boolean regionUndecided,

	@Schema(description = "여행 시작일 (Asia/Seoul 기준 오늘 이후)", example = "2026-07-22", type = "string", format = "date")
	@NotNull(message = "여행 시작일은 필수입니다.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	LocalDate startDate,

	@Schema(description = "여행 종료일 (시작일과 같거나 이후)", example = "2026-07-25", type = "string", format = "date")
	@NotNull(message = "여행 종료일은 필수입니다.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	LocalDate endDate,

	@Schema(description = "하루 활동 시작 시각 (HH:mm)", example = "09:00", type = "string", format = "partial-time")
	@NotNull(message = "활동 시작 시각은 필수입니다.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
	LocalTime activityStartTime,

	@Schema(description = "하루 활동 종료 시각 (HH:mm, 시작 시각보다 이후)", example = "22:00", type = "string", format = "partial-time")
	@NotNull(message = "활동 종료 시각은 필수입니다.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
	LocalTime activityEndTime,

	@Schema(
		description = "주 이동 수단 (정확히 1개)",
		example = "PUBLIC_TRANSIT",
		allowableValues = {"WALKING", "PUBLIC_TRANSIT", "CAR"}
	)
	@NotNull(message = "이동 수단은 필수입니다.")
	TransportMode transportMode,

	@Schema(description = "선택 가능한 TourAPI 관광타입 2~4개와 각 관광타입별 세부 취향 목록")
	@NotEmpty(message = "취향은 최소 1개 이상 선택해야 합니다.")
	List<@Valid @NotNull ContentTypePreferenceRequest> preferences,

	@Schema(
		description = "여행 경험 밀도",
		example = "NORMAL",
		allowableValues = {"LIGHT", "NORMAL", "DEEP"}
	)
	@NotNull(message = "여행 경험 밀도는 필수입니다.")
	ExperienceLevel experienceLevel
) {

	public boolean isRegionUndecided() {
		return Boolean.TRUE.equals(regionUndecided);
	}
}
