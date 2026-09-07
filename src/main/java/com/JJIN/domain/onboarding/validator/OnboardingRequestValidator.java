package com.JJIN.domain.onboarding.validator;

import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.dto.request.OnboardingCompleteRequest;
import com.JJIN.domain.travelplan.validator.TravelPlanRequestValidator;

import lombok.RequiredArgsConstructor;

/**
 * 기존 온보딩 경로의 호환용 검증기.
 * 실제 여행 일정 생성 규칙은 TravelPlanRequestValidator에서 한 번만 관리한다.
 */
@Component
@RequiredArgsConstructor
public class OnboardingRequestValidator {

	private final TravelPlanRequestValidator travelPlanRequestValidator;

	public void validate(final OnboardingCompleteRequest request) {
		travelPlanRequestValidator.validateCreate(request.toCommand());
	}
}
