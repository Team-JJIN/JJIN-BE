package com.JJIN.domain.onboarding.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.onboarding.dto.request.OnboardingCompleteRequest;
import com.JJIN.domain.onboarding.dto.response.OnboardingCompleteResponse;
import com.JJIN.domain.onboarding.dto.response.TravelRegionResponse;
import com.JJIN.domain.onboarding.repository.TravelRegionRepository;
import com.JJIN.domain.travelplan.service.TravelPlanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

	private final TravelRegionRepository travelRegionRepository;
	private final TravelPlanService travelPlanService;

	/**
	 * 기존 클라이언트 호환 경로로 여행 일정을 생성한다.
	 * 신규 클라이언트는 POST /api/travel-plans를 사용한다.
	 */
	@Transactional
	public OnboardingCompleteResponse complete(
		final Long memberId,
		final OnboardingCompleteRequest request
	) {
		Long travelPlanId = travelPlanService.create(memberId, request.toCommand());
		log.info("기존 온보딩 경로를 통한 여행 일정 생성 완료: memberId={}, travelPlanId={}", memberId, travelPlanId);
		return OnboardingCompleteResponse.of(travelPlanId);
	}

	@Transactional(readOnly = true)
	public List<TravelRegionResponse> searchRegions(final String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return List.of();
		}
		return travelRegionRepository.findTop20ByDisplayNameContainingOrderByDisplayNameAsc(keyword.trim())
			.stream()
			.map(TravelRegionResponse::from)
			.toList();
	}

}
