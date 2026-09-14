package com.JJIN.domain.travelplan.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TravelPlanErrorCode implements BaseCode {

	INVALID_REGION_SELECTION(HttpStatus.BAD_REQUEST, "지역 선택과 지역 미정 여부가 올바르지 않습니다."),
	INVALID_TRAVEL_DATE(HttpStatus.BAD_REQUEST, "여행 날짜가 올바르지 않습니다."),
	INVALID_ACTIVITY_TIME(HttpStatus.BAD_REQUEST, "활동 시간이 올바르지 않습니다."),
	INVALID_CONTENT_TYPE_COUNT(HttpStatus.BAD_REQUEST, "선택 가능한 TourAPI 관광타입은 중복 없이 2~4개를 선택해야 합니다."),
	MISSING_SUBCATEGORY(HttpStatus.BAD_REQUEST, "선택한 TourAPI 관광타입에는 세부 취향이 최소 1개 있어야 합니다."),
	INVALID_SUBCATEGORY(HttpStatus.BAD_REQUEST, "TourAPI 관광타입과 세부 취향 선택이 올바르지 않습니다."),
	INVALID_ALL_FOOD_SELECTION(HttpStatus.BAD_REQUEST, "'다 좋아요'는 다른 음식점 세부 취향과 함께 선택할 수 없습니다."),
	TRAVEL_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "여행 일정을 찾을 수 없습니다."),
	TRAVEL_PLAN_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 여행 일정만 조회할 수 있습니다."),
	INVALID_DAY_NUMBER(HttpStatus.BAD_REQUEST, "여행 기간 범위를 벗어난 일차입니다."),
	PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 장소를 찾을 수 없습니다."),
	COURSE_STOP_NOT_FOUND(HttpStatus.NOT_FOUND, "코스 방문지를 찾을 수 없습니다."),
	INVALID_STOP_ORDER(HttpStatus.BAD_REQUEST,
		"순번 요청이 올바르지 않습니다. 같은 일차의 모든 방문지를 1..N 순번으로 빠짐없이 포함해야 합니다."),
	COURSE_GENERATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY,
		"여행 코스를 생성하지 못했습니다. 조건에 맞는 장소 후보가 부족할 수 있습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
