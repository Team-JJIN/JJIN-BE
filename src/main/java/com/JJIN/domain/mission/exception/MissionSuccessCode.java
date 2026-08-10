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
	;

	private final HttpStatus httpStatus;
	private final String message;
}
