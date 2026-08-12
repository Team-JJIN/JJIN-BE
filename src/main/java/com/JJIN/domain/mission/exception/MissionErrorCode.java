package com.JJIN.domain.mission.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionErrorCode implements BaseCode {

	/*
	400 BAD REQUEST
	 */
	MISSION_PROOF_COMMENT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "필수 데이터가 누락되었습니다."),
	MISSION_PROOF_COMMENT_CONTENT_INVALID(HttpStatus.BAD_REQUEST, "요청 필드 값이 유효하지 않습니다."),

	/*
	404 NOT FOUND
	 */
	MISSION_PROOF_NOT_FOUND(HttpStatus.NOT_FOUND, "미션 인증글을 찾을 수 없습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
