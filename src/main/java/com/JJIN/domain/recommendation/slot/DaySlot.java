package com.JJIN.domain.recommendation.slot;

import java.time.LocalTime;
import java.util.Set;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;

/**
 * 코스의 한 자리(빈 슬롯). 장소 정보 없이 '언제, 어떤 카테고리가 들어갈 수 있는지'만 정의한다.
 *
 * @param time          제안 방문 시각
 * @param kind          슬롯 종류
 * @param allowedTypes  이 자리에 들어갈 수 있는 콘텐츠 유형
 * @param cafePreferred 카페(중분류 FD05)를 우선 채울지
 * @param timeSensitive 그 시각에 운영 중인 곳만 채울지(식사·카페처럼 시간이 중요한 자리)
 */
public record DaySlot(
	LocalTime time,
	SlotKind kind,
	Set<TourApiContentType> allowedTypes,
	boolean cafePreferred,
	boolean timeSensitive
) {
}
