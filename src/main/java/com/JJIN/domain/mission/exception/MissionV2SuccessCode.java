package com.JJIN.domain.mission.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionV2SuccessCode implements BaseCode {

	/*
	200 OK
	 */
	MISSION_CREATE_SUCCESS(HttpStatus.OK, "요청이 성공적으로 수행되었습니다."),
	PRESIGNED_URL_SUCCESS(HttpStatus.OK, "Presigned URL이 생성되었습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
