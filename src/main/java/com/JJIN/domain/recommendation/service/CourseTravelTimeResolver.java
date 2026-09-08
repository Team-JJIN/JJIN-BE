package com.JJIN.domain.recommendation.service;

import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.entity.enums.TransportMode;
import com.JJIN.global.geo.GeoPoint;
import com.JJIN.global.kakao.KakaoMobilityClient;
import com.JJIN.global.kakao.dto.Coordinate;
import com.JJIN.global.kakao.dto.RouteEstimate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 코스 검증 단계(P5)의 구간 이동시간 계산.
 * 자동차는 카카오모빌리티 실측을 우선 사용하고, 실패하거나 다른 이동수단이면 근사식으로 대체한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseTravelTimeResolver {

	private final TravelTimeEstimator travelTimeEstimator;
	private final KakaoMobilityClient kakaoMobilityClient;

	public int estimateMinutes(final GeoPoint origin, final GeoPoint destination, final TransportMode mode) {
		if (mode == TransportMode.CAR) {
			try {
				RouteEstimate estimate = kakaoMobilityClient.getDrivingRoute(toCoordinate(origin), toCoordinate(destination));
				return estimate.durationMinutes();
			} catch (RuntimeException e) {
				log.warn("카카오 실측 이동시간 조회 실패, 근사값으로 대체합니다.", e);
			}
		}
		return travelTimeEstimator.estimateMinutes(origin, destination, mode);
	}

	private Coordinate toCoordinate(final GeoPoint point) {
		return new Coordinate(point.longitude(), point.latitude());
	}
}
