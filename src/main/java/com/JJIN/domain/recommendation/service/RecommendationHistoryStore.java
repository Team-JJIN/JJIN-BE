package com.JJIN.domain.recommendation.service;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 최근 추천 이력 저장소. 최근 추천된 장소에 스코어링 패널티를 주기 위해 사용한다.
 */
@Component
@RequiredArgsConstructor
public class RecommendationHistoryStore {

	private static final String KEY_PREFIX = "rec:history:";
	private static final Duration TTL = Duration.ofDays(15);

	private final StringRedisTemplate redisTemplate;

	public void record(final Long memberId, final Collection<Long> placeIds) {
		placeIds.forEach(placeId -> redisTemplate.opsForValue().set(key(memberId, placeId), "1", TTL));
	}

	public Set<Long> findRecentlyRecommended(final Long memberId, final Collection<Long> placeIds) {
		return placeIds.stream()
			.filter(placeId -> Boolean.TRUE.equals(redisTemplate.hasKey(key(memberId, placeId))))
			.collect(Collectors.toSet());
	}

	private String key(final Long memberId, final Long placeId) {
		return KEY_PREFIX + memberId + ":" + placeId;
	}
}
