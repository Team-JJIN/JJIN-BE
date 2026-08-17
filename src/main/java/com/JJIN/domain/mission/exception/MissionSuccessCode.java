package com.JJIN.domain.mission.exception;

import org.springframework.http.HttpStatus;

import com.JJIN.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionSuccessCode implements BaseCode {

	/*
	200 OK
	 */
	MISSION_SEARCH_SUCCESS(HttpStatus.OK, "미션 검색 피드 조회에 성공했습니다."),
	MISSION_DETAIL_SUCCESS(HttpStatus.OK, "미션 상세 조회에 성공했습니다."),
	HOT_MISSION_LIST_SUCCESS(HttpStatus.OK, "요즘 핫한 미션 목록을 조회했습니다."),
	MISSION_PROOF_FEED_SUCCESS(HttpStatus.OK, "미션 인증 피드를 조회했습니다."),
	MISSION_PROOF_LIKE_TOGGLE_SUCCESS(HttpStatus.OK, "미션 인증 좋아요 상태를 변경했습니다."),
	MISSION_PROOF_COMMENT_LIST_SUCCESS(HttpStatus.OK, "미션 인증 댓글 목록을 조회했습니다."),
	MISSION_PROOF_COMMENT_CREATE_SUCCESS(HttpStatus.OK, "미션 인증 댓글을 작성했습니다."),
	MISSION_CREATE_SUCCESS(HttpStatus.OK, "요청이 성공적으로 수행되었습니다."),
	PRESIGNED_URL_SUCCESS(HttpStatus.OK, "Presigned URL이 생성되었습니다."),
	MISSION_ADD_TO_PLAN_SUCCESS(HttpStatus.OK, "찜 설정이 완료되었습니다."),
	MISSION_REMOVE_FROM_PLAN_SUCCESS(HttpStatus.OK, "찜 설정이 해제되었습니다."),
	MISSION_LIKE_STATUS_SUCCESS(HttpStatus.OK, "일정 별 해당 미션의 찜 여부가 조회되었습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
