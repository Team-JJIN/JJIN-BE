package com.JJIN.domain.travelplan.dto.response;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;

import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.entity.enums.TransportMode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "여행 일정 목록 카드")
public record TravelPlanSummaryResponse(
	Long travelPlanId,
	String name,
	LocalDate startDate,
	LocalDate endDate,
	TransportMode transportMode,
	String transportModeDisplayName,
	List<TourApiContentType> interestCategories,
	ExperienceLevel experienceLevel,
	int nights,
	int days
) {

	public static TravelPlanSummaryResponse from(final TravelPlan travelPlan) {
		int days = Math.toIntExact(ChronoUnit.DAYS.between(
			travelPlan.getStartDate(),
			travelPlan.getEndDate()
		)) + 1;

		LinkedHashSet<TourApiContentType> categories = new LinkedHashSet<>();
		travelPlan.getPreferences().forEach(preference ->
			categories.add(preference.getContentType())
		);

		return new TravelPlanSummaryResponse(
			travelPlan.getId(),
			travelPlan.getName(),
			travelPlan.getStartDate(),
			travelPlan.getEndDate(),
			travelPlan.getTransportMode(),
			travelPlan.getTransportMode().getDisplayName(),
			List.copyOf(categories),
			travelPlan.getExperienceLevel(),
			days - 1,
			days
		);
	}
}
