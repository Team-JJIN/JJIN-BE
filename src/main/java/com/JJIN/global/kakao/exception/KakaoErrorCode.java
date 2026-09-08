package com.JJIN.global.kakao.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KakaoErrorCode implements BaseCode {

	/*
	500 INTERNAL SERVER ERROR
	 */
	DIRECTIONS_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "카카오모빌리티 길찾기 요청에 실패했습니다."),
	ROUTE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "두 지점 간 경로를 찾을 수 없습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
