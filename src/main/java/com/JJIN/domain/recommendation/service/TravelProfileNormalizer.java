package com.JJIN.domain.recommendation.service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.recommendation.dto.TravelProfile;
import com.JJIN.domain.recommendation.policy.LocalityPolicy;
import com.JJIN.domain.recommendation.policy.TransportPolicy;

/**
 * 여행 프로파일 정규화.
 * 이동수단 → 구간예산·클러스터 반경·시군구 배수, 레벨 → 목표 로컬도·가중치, 활동시간 → 슬롯 수로 변환한다.
 */
@Component
public class TravelProfileNormalizer {

	private static final int AVG_STAY_MINUTES = 90;
	private static final int MIN_SLOTS = 3;
	private static final int MAX_SLOTS = 7;
	private static final int MIN_DISTRICTS = 3;

	public TravelProfile normalize(final TravelPlan plan) {
		TransportPolicy transport = TransportPolicy.of(plan.getTransportMode());
		LocalityPolicy locality = LocalityPolicy.of(plan.getExperienceLevel());

		int tripDays = (int) ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
		int availableMinutes = (int) Duration.between(
			plan.getActivityStartTime(), plan.getActivityEndTime()).toMinutes();

		int slotCount = calculateSlotCount(availableMinutes, transport.avgTravelMinutes());
		int requiredDistricts = Math.max(MIN_DISTRICTS, tripDays * transport.districtMultiplier());

		return new TravelProfile(
			plan.getTransportMode(),
			plan.getExperienceLevel(),
			plan.getActivityStartTime(),
			plan.getActivityEndTime(),
			tripDays,
			availableMinutes,
			slotCount,
			requiredDistricts,
			transport.legBudgetMinutes(),
			transport.clusterRadiusKm(),
			locality.targetLocality(),
			transport.lambda(),
			locality.categoryWeight(),
			locality.localityWeight(),
			locality.timeWeight()
		);
	}

	private int calculateSlotCount(final int availableMinutes, final int avgTravelMinutes) {
		int avgSlotCost = AVG_STAY_MINUTES + avgTravelMinutes;
		int raw = avgSlotCost <= 0 ? MIN_SLOTS : (int) Math.floor((double) availableMinutes / avgSlotCost);
		return Math.max(MIN_SLOTS, Math.min(MAX_SLOTS, raw));
	}
}
