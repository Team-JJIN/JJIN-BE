package com.JJIN.domain.mission.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.JJIN.domain.mission.controller.docs.MissionControllerDocs;
import com.JJIN.domain.mission.dto.response.HotMissionListResponse;
import com.JJIN.domain.mission.exception.MissionSuccessCode;
import com.JJIN.domain.mission.service.HotMissionService;
import com.JJIN.global.response.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController implements MissionControllerDocs {

	private final HotMissionService hotMissionService;

	@Override
	@GetMapping("/hot")
	public ResponseEntity<SuccessResponse<HotMissionListResponse>> getHotMissions() {
		HotMissionListResponse response = hotMissionService.getCurrentHotMissions();
		return ResponseEntity.ok(SuccessResponse.of(MissionSuccessCode.HOT_MISSION_LIST_SUCCESS, response));
	}
}
