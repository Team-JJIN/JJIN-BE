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
	MISSION_SEARCH_SUCCESS(HttpStatus.OK, "미션 검색 피드 조회에 성공했습니다."),
	MISSION_DETAIL_SUCCESS(HttpStatus.OK, "미션 상세 조회에 성공했습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
