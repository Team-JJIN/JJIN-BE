package com.JJIN.domain.recommendation.policy;

import java.util.EnumMap;
import java.util.Map;

import com.JJIN.domain.onboarding.entity.enums.TransportMode;

/**
 * 이동수단별 정량 상수(기획서 P0). 직선거리 근사 이동시간, 구간예산, 클러스터 반경, 시군구 배수, λ 등을 정의한다.
 *
 * @param effectiveSpeedKmh 유효 평균 속도(km/h)
 * @param detourFactor      우회계수(실거리/직선거리)
 * @param fixedCostMinutes  구간 고정비용(대기·환승·주차 등, 분)
 * @param legBudgetMinutes  한 구간 최대 허용 이동시간(분). 초과 후보는 제외
 * @param clusterRadiusKm   하루 클러스터 반경(km)
 * @param districtMultiplier 여행 일수당 필요 시군구 배수
 * @param avgTravelMinutes  슬롯 수 산정용 평균 이동시간(분)
 * @param lambda            이동시간을 점수로 환산하는 계수(1분 = lambda점 손해)
 */
public record TransportPolicy(
	double effectiveSpeedKmh,
	double detourFactor,
	int fixedCostMinutes,
	int legBudgetMinutes,
	double clusterRadiusKm,
	int districtMultiplier,
	int avgTravelMinutes,
	double lambda
) {

	private static final Map<TransportMode, TransportPolicy> POLICIES = new EnumMap<>(TransportMode.class);

	static {
		POLICIES.put(TransportMode.WALKING,
			new TransportPolicy(4.0, 1.25, 0, 25, 2.0, 1, 15, 2.0));
		POLICIES.put(TransportMode.PUBLIC_TRANSIT,
			new TransportPolicy(18.0, 1.30, 12, 40, 8.0, 2, 25, 1.0));
		POLICIES.put(TransportMode.CAR,
			new TransportPolicy(20.0, 1.35, 10, 40, 25.0, 3, 25, 0.6));
	}

	public static TransportPolicy of(final TransportMode mode) {
		return POLICIES.get(mode);
	}
}
