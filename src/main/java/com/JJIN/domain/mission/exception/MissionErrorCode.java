package com.JJIN.domain.mission.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionErrorCode implements BaseCode {

	/*
	404 NOT FOUND
	 */
	MISSION_PROOF_NOT_FOUND(HttpStatus.NOT_FOUND, "미션 인증글을 찾을 수 없습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
