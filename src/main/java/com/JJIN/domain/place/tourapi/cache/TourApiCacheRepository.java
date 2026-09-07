package com.JJIN.domain.place.tourapi.cache;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.JJIN.domain.place.tourapi.dto.TourApiIntroItem;
import com.JJIN.domain.place.tourapi.dto.TourApiPage;
import com.JJIN.domain.place.tourapi.dto.TourApiPlaceItem;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class TourApiCacheRepository {

	private static final TypeReference<TourApiPage<TourApiPlaceItem>> PLACE_PAGE_TYPE =
		new TypeReference<>() {
		};

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public Optional<TourApiPage<TourApiPlaceItem>> findPlacePage(final String key) {
		String value = redisTemplate.opsForValue().get(key);
		if (value == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(value, PLACE_PAGE_TYPE));
		} catch (JacksonException exception) {
			redisTemplate.delete(key);
			return Optional.empty();
		}
	}

	public void savePlacePage(
		final String key,
		final TourApiPage<TourApiPlaceItem> page,
		final Duration ttl
	) {
		save(key, page, ttl);
	}

	public Optional<TourApiPlaceItem> findCommonDetail(final String key) {
		return find(key, TourApiPlaceItem.class);
	}

	public void saveCommonDetail(
		final String key,
		final TourApiPlaceItem item,
		final Duration ttl
	) {
		save(key, item, ttl);
	}

	public Optional<TourApiIntroItem> findIntroDetail(final String key) {
		return find(key, TourApiIntroItem.class);
	}

	public void saveIntroDetail(
		final String key,
		final TourApiIntroItem item,
		final Duration ttl
	) {
		save(key, item, ttl);
	}

	private <T> Optional<T> find(final String key, final Class<T> type) {
		String value = redisTemplate.opsForValue().get(key);
		if (value == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(value, type));
		} catch (JacksonException exception) {
			redisTemplate.delete(key);
			return Optional.empty();
		}
	}

	private void save(final String key, final Object value, final Duration ttl) {
		try {
			redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
		} catch (JacksonException exception) {
			throw new IllegalStateException("TourAPI 캐시 직렬화에 실패했습니다.", exception);
		}
	}
}
