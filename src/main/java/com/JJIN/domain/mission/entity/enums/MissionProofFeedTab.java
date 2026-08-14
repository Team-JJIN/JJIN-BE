package com.JJIN.domain.mission.entity.enums;

/**
 * 미션 인증 피드 상단 탭. 단일 선택이며 선택된 탭 기준으로 피드가 정렬/필터링된다.
 */
public enum MissionProofFeedTab {

	/** 최신순 */
	LATEST,
	/** 인기순(좋아요 많은 순) */
	POPULAR,
	/** 이번 주 완료 수가 많은 미션의 인증글 우선 */
	WEEKLY_HOT,
	/** 현재 로그인 사용자가 완료한 미션의 인증글 */
	COMPLETED,
	;
}
