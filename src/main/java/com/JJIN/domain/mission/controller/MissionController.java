package com.JJIN.domain.mission.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.JJIN.domain.mission.controller.docs.MissionControllerDocs;
import com.JJIN.domain.mission.dto.response.MissionDetailResponse;
import com.JJIN.domain.mission.dto.response.MissionSearchFeedResponse;
import com.JJIN.domain.mission.exception.MissionSuccessCode;
import com.JJIN.domain.mission.service.MissionService;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.global.auth.annotation.CurrentMember;
import com.JJIN.global.auth.dto.CurrentAuth;
import com.JJIN.global.auth.jwt.exception.TokenErrorCode;
import com.JJIN.global.exception.JjinException;
import com.JJIN.global.response.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController implements MissionControllerDocs {

	private final MissionService missionService;

	@Override
	@GetMapping
	public ResponseEntity<SuccessResponse<MissionSearchFeedResponse>> searchMissions(
		@CurrentMember CurrentAuth currentAuth,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) List<TourApiContentType> categories,
		@RequestParam(required = false) List<MissionDifficulty> difficulties,
		@RequestParam(defaultValue = "popular") String sort,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		validateAuthenticated(currentAuth);
		MissionSearchFeedResponse response = missionService.searchMissions(
			currentAuth.memberId(),
			keyword,
			categories,
			difficulties,
			sort,
			page,
			size
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

	private void validateAuthenticated(final CurrentAuth currentAuth) {
		if (currentAuth == null) {
			throw new JjinException(TokenErrorCode.INVALID_AUTHORIZATION_HEADER);
		}
	}
}
