package com.JJIN.domain.recommendation.service;

import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.entity.enums.TransportMode;
import com.JJIN.domain.recommendation.policy.TransportPolicy;
import com.JJIN.global.geo.GeoPoint;
import com.JJIN.global.geo.GeoUtils;

/**
 * 두 지점 간 이동시간 추정.
 * 기본은 직선거리 × 우회계수 ÷ 유효속도 근사식이며, 실제 도로 기준 실측(자동차)은
 * 검증 단계에서 카카오모빌리티로 별도 보정한다.
 */
@Component
public class TravelTimeEstimator {

	/**
	 * 이동수단별 근사 이동시간(분). 하드 필터(구간예산)·빔서치·슬롯 산정에 사용한다.
	 */
	public int estimateMinutes(final GeoPoint origin, final GeoPoint destination, final TransportMode mode) {
		TransportPolicy policy = TransportPolicy.of(mode);
		double straightKm = GeoUtils.haversineKm(origin, destination);
		double minutes = policy.fixedCostMinutes()
			+ (straightKm * policy.detourFactor()) / policy.effectiveSpeedKmh() * 60.0;
		return (int) Math.ceil(minutes);
	}
}
