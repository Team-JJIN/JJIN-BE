package com.JJIN.domain.onboarding.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OnboardingSuccessCode implements BaseCode {

	/*
	201 CREATED
	 */
	ONBOARDING_COMPLETE_SUCCESS(HttpStatus.CREATED, "첫 여행 일정을 생성했습니다."),

	/*
	200 OK
	 */
	REGION_LIST_SUCCESS(HttpStatus.OK, "여행 지역 검색에 성공했습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
