package com.JJIN.domain.mission.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.JJIN.domain.mission.dto.request.CreateMissionRequest;
import com.JJIN.domain.mission.dto.request.PresignedUrlRequest;
import com.JJIN.domain.mission.dto.response.CreateMissionResponse;
import com.JJIN.domain.mission.dto.response.PresignedUrlResponse;
import com.JJIN.domain.mission.exception.MissionV2SuccessCode;
import com.JJIN.domain.mission.service.MissionV2Service;
import com.JJIN.global.auth.annotation.CurrentMember;
import com.JJIN.global.auth.dto.CurrentAuth;
import com.JJIN.global.auth.jwt.exception.TokenErrorCode;
import com.JJIN.global.exception.JjinException;
import com.JJIN.global.response.dto.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MissionV2Controller {

	private final MissionV2Service missionV2Service;

	/** 새로운 미션 생성 */
	@PostMapping("/api/missions")
	public ResponseEntity<SuccessResponse<CreateMissionResponse>> createMission(
		@CurrentMember CurrentAuth currentAuth,
		@Valid @RequestBody CreateMissionRequest request
	) {
		Long memberId = requireMemberId(currentAuth);
		Long missionId = missionV2Service.createMission(memberId, request);
		return ResponseEntity.ok(
			SuccessResponse.of(MissionV2SuccessCode.MISSION_CREATE_SUCCESS, CreateMissionResponse.of(missionId))
		);
	}

	/** 미션 이미지 업로드용 presigned URL 발급 */
	@PostMapping("/api/mission/presigned-url")
	public ResponseEntity<SuccessResponse<PresignedUrlResponse>> createPresignedUrl(
		@CurrentMember CurrentAuth currentAuth,
		@Valid @RequestBody PresignedUrlRequest request
	) {
		requireMemberId(currentAuth);
		PresignedUrlResponse response = missionV2Service.createPresignedUrl(request.fileName(), request.contentType());
		return ResponseEntity.ok(SuccessResponse.of(MissionV2SuccessCode.PRESIGNED_URL_SUCCESS, response));
	}

	private Long requireMemberId(final CurrentAuth currentAuth) {
		if (currentAuth == null) {
			throw new JjinException(TokenErrorCode.INVALID_AUTHORIZATION_HEADER);
		}
		return currentAuth.memberId();
	}
}
