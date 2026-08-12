package com.JJIN.domain.mission.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionSuccessCode implements BaseCode {

	/*
	200 OK
	 */
	HOT_MISSION_LIST_SUCCESS(HttpStatus.OK, "요즘 핫한 미션 목록을 조회했습니다."),
	MISSION_PROOF_FEED_SUCCESS(HttpStatus.OK, "미션 인증 피드를 조회했습니다."),
	MISSION_PROOF_LIKE_TOGGLE_SUCCESS(HttpStatus.OK, "미션 인증 좋아요 상태를 변경했습니다."),
	MISSION_PROOF_COMMENT_LIST_SUCCESS(HttpStatus.OK, "미션 인증 댓글 목록을 조회했습니다."),
	MISSION_PROOF_COMMENT_CREATE_SUCCESS(HttpStatus.OK, "미션 인증 댓글을 작성했습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
