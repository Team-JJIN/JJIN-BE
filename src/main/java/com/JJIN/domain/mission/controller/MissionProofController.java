package com.JJIN.domain.mission.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.JJIN.domain.mission.controller.docs.MissionProofControllerDocs;
import com.JJIN.domain.mission.dto.response.MissionProofFeedResponse;
import com.JJIN.domain.mission.dto.response.MissionProofLikeToggleResponse;
import com.JJIN.domain.mission.entity.enums.MissionProofFeedTab;
import com.JJIN.domain.mission.exception.MissionSuccessCode;
import com.JJIN.domain.mission.service.MissionProofService;
import com.JJIN.global.auth.annotation.CurrentMember;
import com.JJIN.global.auth.dto.CurrentAuth;
import com.JJIN.global.auth.jwt.exception.TokenErrorCode;
import com.JJIN.global.exception.JjinException;
import com.JJIN.global.response.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/missions/proofs")
@RequiredArgsConstructor
public class MissionProofController implements MissionProofControllerDocs {

	private final MissionProofService missionProofService;

	@Override
	@GetMapping("/feed")
	public ResponseEntity<SuccessResponse<MissionProofFeedResponse>> getMissionProofFeed(
		@CurrentMember CurrentAuth currentAuth,
		@RequestParam(defaultValue = "LATEST") MissionProofFeedTab tab,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		if (currentAuth == null) {
			throw new JjinException(TokenErrorCode.INVALID_AUTHORIZATION_HEADER);
		}
		MissionProofFeedResponse response =
			missionProofService.getFeed(currentAuth.memberId(), tab, page, size);
		return ResponseEntity.ok(
			SuccessResponse.of(MissionSuccessCode.MISSION_PROOF_FEED_SUCCESS, response));
	}

	@Override
	@PostMapping("/{proofId}/likes/toggle")
	public ResponseEntity<SuccessResponse<MissionProofLikeToggleResponse>> toggleMissionProofLike(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long proofId
	) {
		if (currentAuth == null) {
			throw new JjinException(TokenErrorCode.INVALID_AUTHORIZATION_HEADER);
		}
		MissionProofLikeToggleResponse response =
			missionProofService.toggleLike(currentAuth.memberId(), proofId);
		return ResponseEntity.ok(
			SuccessResponse.of(MissionSuccessCode.MISSION_PROOF_LIKE_TOGGLE_SUCCESS, response));
	}
}
