package com.JJIN.domain.place.entity.enums;

/**
 * 요청 시각을 기준으로 계산하는 장소 운영 상태.
 * 영속화하지 않고 응답 생성 시 계산한다.
 */
public enum OpenStatus {

	OPEN,
	CLOSED,
	BREAK,
	UNKNOWN,
}
