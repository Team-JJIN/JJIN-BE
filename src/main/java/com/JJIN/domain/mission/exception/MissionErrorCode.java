package com.JJIN.domain.mission.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionErrorCode implements BaseCode {

	/*
	400 BAD REQUEST
	 */
	INVALID_SORT_OPTION(HttpStatus.BAD_REQUEST, "지원하지 않는 미션 정렬값입니다."),
	INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "페이지 요청 값이 올바르지 않습니다."),
	MISSION_PROOF_COMMENT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "필수 데이터가 누락되었습니다."),
	MISSION_PROOF_COMMENT_CONTENT_INVALID(HttpStatus.BAD_REQUEST, "요청 필드 값이 유효하지 않습니다."),
	UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),

	/*
	403 FORBIDDEN
	 */
	NOT_PLAN_OWNER(HttpStatus.FORBIDDEN, "본인의 일정에만 미션을 추가할 수 있습니다."),

	/*
	404 NOT FOUND
	 */
	MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "미션을 찾을 수 없습니다."),
	MISSION_PROOF_NOT_FOUND(HttpStatus.NOT_FOUND, "미션 인증글을 찾을 수 없습니다."),
	TRAVEL_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "여행 일정을 찾을 수 없습니다."),

	/*
	500 INTERNAL SERVER ERROR
	 */
	PRESIGNED_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Presigned URL 생성에 실패했습니다."),
	CATEGORY_CLASSIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "미션 카테고리 분류에 실패했습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
