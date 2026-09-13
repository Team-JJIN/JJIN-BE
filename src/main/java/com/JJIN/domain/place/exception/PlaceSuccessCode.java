package com.JJIN.domain.place.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceSuccessCode implements BaseCode {

	PLACE_SEARCH_SUCCESS(HttpStatus.OK, "장소를 검색했습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
