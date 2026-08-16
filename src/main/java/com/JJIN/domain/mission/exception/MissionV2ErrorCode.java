package com.JJIN.domain.mission.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionV2ErrorCode implements BaseCode {

	/*
	400 BAD REQUEST
	 */
	UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),

	/*
	500 INTERNAL SERVER ERROR
	 */
	PRESIGNED_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Presigned URL 생성에 실패했습니다."),
	CATEGORY_CLASSIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "미션 카테고리 분류에 실패했습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
