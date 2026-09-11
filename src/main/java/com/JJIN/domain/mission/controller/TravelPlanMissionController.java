package com.JJIN.domain.mission.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.JJIN.domain.mission.controller.docs.TravelPlanMissionControllerDocs;
import com.JJIN.domain.mission.dto.response.TravelPlanMissionListResponse;
import com.JJIN.domain.mission.entity.enums.UserMissionStatus;
import com.JJIN.domain.mission.exception.MissionSuccessCode;
import com.JJIN.domain.mission.service.TravelPlanMissionService;
import com.JJIN.global.auth.annotation.CurrentMember;
import com.JJIN.global.auth.dto.CurrentAuth;
import com.JJIN.global.auth.jwt.exception.TokenErrorCode;
import com.JJIN.global.exception.JjinException;
import com.JJIN.global.response.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/travel-plans/{travelPlanId}/missions")
@RequiredArgsConstructor
public class TravelPlanMissionController implements TravelPlanMissionControllerDocs {

	private final TravelPlanMissionService travelPlanMissionService;

	@Override
	@GetMapping
	public ResponseEntity<SuccessResponse<TravelPlanMissionListResponse>> getTravelPlanMissions(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long travelPlanId,
		@RequestParam(required = false) UserMissionStatus status
	) {
		validateCurrentAuth(currentAuth);
		return ResponseEntity.ok(
			SuccessResponse.of(
				MissionSuccessCode.TRAVEL_PLAN_MISSION_LIST_SUCCESS,
				travelPlanMissionService.getTravelPlanMissions(currentAuth.memberId(), travelPlanId, status)
			)
		);
	}

	@Override
	@DeleteMapping("/{userMissionId}")
	public ResponseEntity<SuccessResponse<Void>> deleteTravelPlanMission(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long travelPlanId,
		@PathVariable Long userMissionId
	) {
		validateCurrentAuth(currentAuth);
		travelPlanMissionService.deleteTravelPlanMission(currentAuth.memberId(), travelPlanId, userMissionId);
		return ResponseEntity.ok(SuccessResponse.of(MissionSuccessCode.TRAVEL_PLAN_MISSION_DELETE_SUCCESS));
	}

	private void validateCurrentAuth(final CurrentAuth currentAuth) {
		if (currentAuth == null) {
			throw new JjinException(TokenErrorCode.INVALID_AUTHORIZATION_HEADER);
		}
	}
}
