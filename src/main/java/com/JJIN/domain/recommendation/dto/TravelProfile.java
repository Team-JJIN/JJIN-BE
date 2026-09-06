package com.JJIN.domain.recommendation.dto;

import java.time.LocalTime;

import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.onboarding.entity.enums.TransportMode;

/**
 * 여행 정보(TravelPlan)를 알고리즘 수식에 넣을 수 있도록 정규화한 프로파일.
 */
public record TravelProfile(
	TransportMode transportMode,
	ExperienceLevel experienceLevel,
	LocalTime activityStartTime,
	LocalTime activityEndTime,
	int tripDays,
	int availableMinutes,
	int slotCount,
	int requiredDistrictCount,
	int legBudgetMinutes,
	double clusterRadiusKm,
	double targetLocality,
	double lambda,
	double categoryWeight,
	double localityWeight,
	double timeWeight
) {
}
