package com.JJIN.domain.travelplan.dto.internal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.entity.enums.TransportMode;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;

/**
 * 화면이나 API 경로와 무관하게 여행 일정 생성에 필요한 값.
 */
public record CreateTravelPlanCommand(
	String name,
	Long regionId,
	Boolean regionUndecided,
	LocalDate startDate,
	LocalDate endDate,
	LocalTime activityStartTime,
	LocalTime activityEndTime,
	TransportMode transportMode,
	List<Preference> preferences,
	ExperienceLevel experienceLevel
) {

	public boolean isRegionUndecided() {
		return Boolean.TRUE.equals(regionUndecided);
	}

	public record Preference(
		TourApiContentType contentType,
		List<TravelSubcategory> subcategories
	) {
	}
}
