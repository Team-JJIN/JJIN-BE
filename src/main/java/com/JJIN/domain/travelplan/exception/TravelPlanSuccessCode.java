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
	TRAVEL_COURSE_DAY_SUCCESS(HttpStatus.OK, "여행 코스 일차별 방문지 목록을 조회했습니다."),
	COURSE_STOP_ADD_SUCCESS(HttpStatus.CREATED, "여행 코스에 방문지를 추가했습니다."),
	COURSE_STOP_DELETE_SUCCESS(HttpStatus.OK, "여행 코스에서 방문지를 삭제했습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
