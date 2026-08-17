package com.JJIN.domain.mission.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.JJIN.domain.mission.controller.docs.MissionControllerDocs;
import com.JJIN.domain.mission.dto.request.AddMissionToPlansRequest;
import com.JJIN.domain.mission.dto.request.CreateMissionRequest;
import com.JJIN.domain.mission.dto.request.PresignedUrlRequest;
import com.JJIN.domain.mission.dto.request.RemoveMissionFromPlansRequest;
import com.JJIN.domain.mission.dto.response.AddMissionToPlansResponse;
import com.JJIN.domain.mission.dto.response.CreateMissionResponse;
import com.JJIN.domain.mission.dto.response.HotMissionListResponse;
import com.JJIN.domain.mission.dto.response.MissionDetailResponse;
import com.JJIN.domain.mission.dto.response.MissionLikeStatusResponse;
import com.JJIN.domain.mission.dto.response.MissionSearchFeedResponse;
import com.JJIN.domain.mission.dto.response.PresignedUrlResponse;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.mission.entity.enums.MissionSourceTypeOption;
import com.JJIN.domain.mission.exception.MissionSuccessCode;
import com.JJIN.domain.mission.service.HotMissionService;
import com.JJIN.domain.mission.service.MissionService;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.global.auth.annotation.CurrentMember;
import com.JJIN.global.auth.dto.CurrentAuth;
import com.JJIN.global.auth.jwt.exception.TokenErrorCode;
import com.JJIN.global.exception.JjinException;
import com.JJIN.global.response.dto.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController implements MissionControllerDocs {

	private final MissionService missionService;
	private final HotMissionService hotMissionService;

	@Override
	@GetMapping
	public ResponseEntity<SuccessResponse<MissionSearchFeedResponse>> searchMissions(
		@CurrentMember CurrentAuth currentAuth,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) List<TourApiContentType> categories,
		@RequestParam(required = false) List<MissionDifficulty> difficulties,
		@RequestParam(defaultValue = "popular") String sort,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "ALL") MissionSourceTypeOption source
	) {
		validateAuthenticated(currentAuth);
		MissionSearchFeedResponse response = missionService.searchMissions(
			currentAuth.memberId(),
			keyword,
			categories,
			difficulties,
			sort,
			page,
			size,
			source
		);
		return ResponseEntity.ok(SuccessResponse.of(MissionSuccessCode.MISSION_SEARCH_SUCCESS, response));
	}

	@Override
	@GetMapping("/{missionId}")
	public ResponseEntity<SuccessResponse<MissionDetailResponse>> getMission(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long missionId
	) {
		validateAuthenticated(currentAuth);
		MissionDetailResponse response = missionService.getMission(currentAuth.memberId(), missionId);
		return ResponseEntity.ok(SuccessResponse.of(MissionSuccessCode.MISSION_DETAIL_SUCCESS, response));
	}

	@Override
	@GetMapping("/hot")
	public ResponseEntity<SuccessResponse<HotMissionListResponse>> getHotMissions() {
		HotMissionListResponse response = hotMissionService.getCurrentHotMissions();
		return ResponseEntity.ok(SuccessResponse.of(MissionSuccessCode.HOT_MISSION_LIST_SUCCESS, response));
	}

	@PostMapping
	public ResponseEntity<SuccessResponse<CreateMissionResponse>> createMission(
		@CurrentMember CurrentAuth currentAuth,
		@Valid @RequestBody CreateMissionRequest request
	) {
		validateAuthenticated(currentAuth);
		Long missionId = missionService.createMission(currentAuth.memberId(), request);
		return ResponseEntity.ok(
			SuccessResponse.of(MissionSuccessCode.MISSION_CREATE_SUCCESS, CreateMissionResponse.of(missionId))
		);
	}

	@GetMapping("/likes/{missionId}")
	public ResponseEntity<SuccessResponse<MissionLikeStatusResponse>> getMissionLikeStatus(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long missionId
	) {
		validateAuthenticated(currentAuth);
		MissionLikeStatusResponse response = missionService.getMissionLikeStatus(currentAuth.memberId(), missionId);
		return ResponseEntity.ok(SuccessResponse.of(MissionSuccessCode.MISSION_LIKE_STATUS_SUCCESS, response));
	}

	@PostMapping("/{missionId}")
	public ResponseEntity<SuccessResponse<AddMissionToPlansResponse>> addMissionToPlans(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long missionId,
		@Valid @RequestBody AddMissionToPlansRequest request
	) {
		validateAuthenticated(currentAuth);
		AddMissionToPlansResponse response =
			missionService.addMissionToPlans(currentAuth.memberId(), missionId, request.planIds());
		return ResponseEntity.ok(SuccessResponse.of(MissionSuccessCode.MISSION_ADD_TO_PLAN_SUCCESS, response));
	}

	@DeleteMapping("/{missionId}")
	public ResponseEntity<SuccessResponse<Void>> removeMissionFromPlans(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long missionId,
		@Valid @RequestBody RemoveMissionFromPlansRequest request
	) {
		validateAuthenticated(currentAuth);
		missionService.removeMissionFromPlans(currentAuth.memberId(), missionId, request.planIds());
		return ResponseEntity.ok(SuccessResponse.of(MissionSuccessCode.MISSION_REMOVE_FROM_PLAN_SUCCESS));
	}

	@PostMapping("/presigned-url")
	public ResponseEntity<SuccessResponse<PresignedUrlResponse>> createPresignedUrl(
		@CurrentMember CurrentAuth currentAuth,
		@Valid @RequestBody PresignedUrlRequest request
	) {
		validateAuthenticated(currentAuth);
		PresignedUrlResponse response = missionService.createPresignedUrl(request.fileName(), request.contentType());
		return ResponseEntity.ok(SuccessResponse.of(MissionSuccessCode.PRESIGNED_URL_SUCCESS, response));
	}

	private void validateAuthenticated(final CurrentAuth currentAuth) {
		if (currentAuth == null) {
			throw new JjinException(TokenErrorCode.INVALID_AUTHORIZATION_HEADER);
		}
	}
}
