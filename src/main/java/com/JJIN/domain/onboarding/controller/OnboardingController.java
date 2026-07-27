package com.JJIN.domain.onboarding.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.JJIN.domain.onboarding.controller.docs.OnboardingControllerDocs;
import com.JJIN.domain.onboarding.dto.request.OnboardingCompleteRequest;
import com.JJIN.domain.onboarding.dto.response.OnboardingCompleteResponse;
import com.JJIN.domain.onboarding.dto.response.TravelRegionResponse;
import com.JJIN.domain.onboarding.exception.OnboardingSuccessCode;
import com.JJIN.domain.onboarding.service.OnboardingService;
import com.JJIN.global.auth.annotation.CurrentMember;
import com.JJIN.global.auth.dto.CurrentAuth;
import com.JJIN.global.auth.jwt.exception.TokenErrorCode;
import com.JJIN.global.exception.JjinException;
import com.JJIN.global.response.dto.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController implements OnboardingControllerDocs {

	private final OnboardingService onboardingService;

	@Override
	@PostMapping
	public ResponseEntity<SuccessResponse<OnboardingCompleteResponse>> completeOnboarding(
		@CurrentMember CurrentAuth currentAuth,
		@Valid @RequestBody OnboardingCompleteRequest request
	) {
		if (currentAuth == null) {
			throw new JjinException(TokenErrorCode.INVALID_AUTHORIZATION_HEADER);
		}
		OnboardingCompleteResponse response = onboardingService.complete(currentAuth.memberId(), request);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(SuccessResponse.of(OnboardingSuccessCode.ONBOARDING_COMPLETE_SUCCESS, response));
	}

	@Override
	@GetMapping("/regions")
	public ResponseEntity<SuccessResponse<List<TravelRegionResponse>>> searchRegions(
		@RequestParam(required = false) String keyword
	) {
		List<TravelRegionResponse> response = onboardingService.searchRegions(keyword);
		return ResponseEntity.ok(SuccessResponse.of(OnboardingSuccessCode.REGION_LIST_SUCCESS, response));
	}
}
