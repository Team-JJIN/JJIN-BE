package com.JJIN.domain.travelplan.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.JJIN.domain.place.entity.enums.PlaceLocale;

import com.JJIN.domain.travelplan.controller.docs.TravelPlanControllerDocs;
import com.JJIN.domain.travelplan.dto.request.AddCourseStopRequest;
import com.JJIN.domain.travelplan.dto.request.CreateTravelPlanRequest;
import com.JJIN.domain.travelplan.dto.request.ReorderCourseStopsRequest;
import com.JJIN.domain.travelplan.dto.response.AddCourseStopResponse;
import com.JJIN.domain.travelplan.dto.response.CreateTravelPlanResponse;
import com.JJIN.domain.travelplan.dto.response.TravelCourseDayResponse;
import com.JJIN.domain.travelplan.dto.response.TravelPlanListResponse;
import com.JJIN.domain.travelplan.exception.TravelPlanSuccessCode;
import com.JJIN.domain.travelplan.service.TravelCourseService;
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
	private final TravelCourseService travelCourseService;

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

	@Override
	@GetMapping("/{planId}/course")
	public ResponseEntity<SuccessResponse<TravelCourseDayResponse>> getCourseDay(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long planId,
		@RequestParam(name = "dayNumber", defaultValue = "1") int dayNumber,
		@RequestParam(name = "locale", defaultValue = "KO") PlaceLocale locale
	) {
		validateCurrentAuth(currentAuth);

		return ResponseEntity.ok(
			SuccessResponse.of(
				TravelPlanSuccessCode.TRAVEL_COURSE_DAY_SUCCESS,
				travelCourseService.getCourseDay(currentAuth.memberId(), planId, dayNumber, locale)
			)
		);
	}

	@Override
	@PostMapping("/{planId}/course/stops")
	public ResponseEntity<SuccessResponse<AddCourseStopResponse>> addCourseStop(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long planId,
		@Valid @RequestBody AddCourseStopRequest request
	) {
		validateCurrentAuth(currentAuth);

		AddCourseStopResponse response = travelCourseService.addStop(
			currentAuth.memberId(), planId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(
			SuccessResponse.of(TravelPlanSuccessCode.COURSE_STOP_ADD_SUCCESS, response)
		);
	}

	@Override
	@PatchMapping("/{planId}/course/stops/order")
	public ResponseEntity<SuccessResponse<Void>> reorderCourseStops(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long planId,
		@Valid @RequestBody ReorderCourseStopsRequest request
	) {
		validateCurrentAuth(currentAuth);

		travelCourseService.reorderStops(currentAuth.memberId(), planId, request);
		return ResponseEntity.ok(
			SuccessResponse.of(TravelPlanSuccessCode.COURSE_STOP_REORDER_SUCCESS)
		);
	}

	@Override
	@DeleteMapping("/{planId}/course/stops/{stopId}")
	public ResponseEntity<SuccessResponse<Void>> deleteCourseStop(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long planId,
		@PathVariable Long stopId
	) {
		validateCurrentAuth(currentAuth);

		travelCourseService.deleteStop(currentAuth.memberId(), planId, stopId);
		return ResponseEntity.ok(
			SuccessResponse.of(TravelPlanSuccessCode.COURSE_STOP_DELETE_SUCCESS)
		);
	}

	private void validateCurrentAuth(final CurrentAuth currentAuth) {
		if (currentAuth == null) {
			throw new JjinException(TokenErrorCode.INVALID_AUTHORIZATION_HEADER);
		}
	}
}
