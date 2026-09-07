package com.JJIN.domain.travelplan.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.JJIN.domain.travelplan.controller.docs.TravelPlanControllerDocs;
import com.JJIN.domain.travelplan.dto.request.CreateTravelPlanRequest;
import com.JJIN.domain.travelplan.dto.response.CreateTravelPlanResponse;
import com.JJIN.domain.travelplan.dto.response.TravelPlanListResponse;
import com.JJIN.domain.travelplan.exception.TravelPlanSuccessCode;
import com.JJIN.domain.travelplan.service.TravelPlanService;
import com.JJIN.global.auth.annotation.CurrentMember;
import com.JJIN.global.auth.dto.CurrentAuth;
import com.JJIN.global.auth.jwt.exception.TokenErrorCode;
import com.JJIN.global.exception.JjinException;
import com.JJIN.global.response.dto.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/travel-plans")
@RequiredArgsConstructor
public class TravelPlanController implements TravelPlanControllerDocs {

	private final TravelPlanService travelPlanService;

	@Override
	@GetMapping
	public ResponseEntity<SuccessResponse<TravelPlanListResponse>> getTravelPlans(
		@CurrentMember CurrentAuth currentAuth
	) {
		validateCurrentAuth(currentAuth);

		return ResponseEntity.ok(
			SuccessResponse.of(
				TravelPlanSuccessCode.TRAVEL_PLAN_LIST_SUCCESS,
				travelPlanService.getTravelPlans(currentAuth.memberId())
			)
		);
	}

	@Override
	@PostMapping
	public ResponseEntity<SuccessResponse<CreateTravelPlanResponse>> createTravelPlan(
		@CurrentMember CurrentAuth currentAuth,
		@Valid @RequestBody CreateTravelPlanRequest request
	) {
		validateCurrentAuth(currentAuth);

		Long travelPlanId = travelPlanService.create(currentAuth.memberId(), request.toCommand());
		return ResponseEntity.status(HttpStatus.CREATED).body(
			SuccessResponse.of(
				TravelPlanSuccessCode.TRAVEL_PLAN_CREATE_SUCCESS,
				CreateTravelPlanResponse.of(travelPlanId)
			)
		);
	}

	private void validateCurrentAuth(final CurrentAuth currentAuth) {
		if (currentAuth == null) {
			throw new JjinException(TokenErrorCode.INVALID_AUTHORIZATION_HEADER);
		}
	}
}
