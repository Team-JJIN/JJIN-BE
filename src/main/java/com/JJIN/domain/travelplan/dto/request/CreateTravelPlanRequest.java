package com.JJIN.domain.travelplan.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.onboarding.entity.enums.TransportMode;
import com.JJIN.domain.travelplan.dto.internal.CreateTravelPlanCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "여행 일정 생성 요청")
public record CreateTravelPlanRequest(

	@Schema(description = "여행 이름", example = "여름 마카오 여행")
	@NotBlank(message = "여행 이름은 비울 수 없습니다.")
	String name,

	@Schema(description = "여행 지역 ID. regionUndecided가 true면 null", example = "1", nullable = true)
	Long regionId,

	@Schema(description = "지역 미정 여부", example = "false")
	@NotNull(message = "지역 미정 여부는 필수입니다.")
	Boolean regionUndecided,

	@Schema(description = "여행 시작일", example = "2026-09-10", type = "string", format = "date")
	@NotNull(message = "여행 시작일은 필수입니다.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	LocalDate startDate,

	@Schema(description = "여행 종료일", example = "2026-09-12", type = "string", format = "date")
	@NotNull(message = "여행 종료일은 필수입니다.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	LocalDate endDate,

	@Schema(description = "하루 활동 시작 시각", example = "09:00", type = "string", format = "partial-time")
	@NotNull(message = "활동 시작 시각은 필수입니다.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
	LocalTime activityStartTime,

	@Schema(description = "하루 활동 종료 시각", example = "21:00", type = "string", format = "partial-time")
	@NotNull(message = "활동 종료 시각은 필수입니다.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
	LocalTime activityEndTime,

	@Schema(description = "주 이동 수단", example = "PUBLIC_TRANSIT")
	@NotNull(message = "이동 수단은 필수입니다.")
	TransportMode transportMode,

	@Schema(description = "관광타입 2~4개와 각 타입의 세부 취향")
	@NotEmpty(message = "취향은 최소 1개 이상 선택해야 합니다.")
	List<@Valid @NotNull TravelPlanPreferenceRequest> preferences,

	@Schema(description = "여행 경험 밀도", example = "NORMAL")
	@NotNull(message = "여행 경험 밀도는 필수입니다.")
	ExperienceLevel experienceLevel
) {

	public CreateTravelPlanCommand toCommand() {
		return new CreateTravelPlanCommand(
			name,
			regionId,
			regionUndecided,
			startDate,
			endDate,
			activityStartTime,
			activityEndTime,
			transportMode,
			preferences == null ? null : preferences.stream()
				.map(TravelPlanPreferenceRequest::toCommand)
				.toList(),
			experienceLevel
		);
	}
}
