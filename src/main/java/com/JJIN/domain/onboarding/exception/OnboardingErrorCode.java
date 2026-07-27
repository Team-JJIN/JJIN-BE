package com.JJIN.domain.onboarding.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OnboardingErrorCode implements BaseCode {

	/*
	400 BAD REQUEST
	 */
	INVALID_REGION_SELECTION(HttpStatus.BAD_REQUEST, "지역 선택과 지역 미정 여부가 올바르지 않습니다."),
	INVALID_TRAVEL_DATE(HttpStatus.BAD_REQUEST, "여행 날짜가 올바르지 않습니다."),
	INVALID_ACTIVITY_TIME(HttpStatus.BAD_REQUEST, "활동 시간이 올바르지 않습니다."),
	INVALID_CONTENT_TYPE_COUNT(HttpStatus.BAD_REQUEST, "선택 가능한 TourAPI 관광타입은 중복 없이 2~4개를 선택해야 합니다."),
	MISSING_SUBCATEGORY(HttpStatus.BAD_REQUEST, "선택한 TourAPI 관광타입에는 세부 취향이 최소 1개 있어야 합니다."),
	INVALID_SUBCATEGORY(HttpStatus.BAD_REQUEST, "TourAPI 관광타입과 세부 취향 선택이 올바르지 않습니다."),
	INVALID_ALL_FOOD_SELECTION(HttpStatus.BAD_REQUEST, "'다 좋아요'는 다른 음식점 세부 취향과 함께 선택할 수 없습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
