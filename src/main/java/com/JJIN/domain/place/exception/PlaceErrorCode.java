package com.JJIN.domain.place.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements BaseCode {

	INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "검색어는 비어 있을 수 없습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
