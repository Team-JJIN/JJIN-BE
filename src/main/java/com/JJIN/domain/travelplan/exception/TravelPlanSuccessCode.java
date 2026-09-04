package com.JJIN.domain.travelplan.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TravelPlanSuccessCode implements BaseCode {

	TRAVEL_PLAN_LIST_SUCCESS(HttpStatus.OK, "여행 일정 목록을 조회했습니다."),
	TRAVEL_PLAN_CREATE_SUCCESS(HttpStatus.CREATED, "여행 일정을 생성했습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
