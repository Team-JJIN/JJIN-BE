package com.JJIN.domain.recommendation.locality.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.JJIN.domain.recommendation.locality.dto.DistrictConcentration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 시군구 방문자 집중도 스냅샷 캐시. Redis TTL 기반 lazy caching.
 * 키 형태: {@code locality:visitor:{signguCode}}
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LocalityScoreCache {

	private static final String KEY_PREFIX = "locality:visitor:";
	private static final String ATTRACTION_KEY_PREFIX = "locality:attraction:";
	private static final TypeReference<Map<String, Double>> ATTRACTION_MAP_TYPE = new TypeReference<>() {
	};

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public Optional<DistrictConcentration> find(final String districtCode) {
		String key = key(districtCode);
		try {
			String value = redisTemplate.opsForValue().get(key);
			if (value == null) {
				return Optional.empty();
			}
			return Optional.of(objectMapper.readValue(value, DistrictConcentration.class));
		} catch (JacksonException exception) {
			redisTemplate.delete(key);
			return Optional.empty();
		} catch (RuntimeException exception) {
			log.warn("로컬도 캐시 조회 실패: key={}", key, exception);
			return Optional.empty();
		}
	}

	public void save(final DistrictConcentration snapshot, final Duration ttl) {
		String key = key(snapshot.districtCode());
		try {
			redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(snapshot), ttl);
		} catch (JacksonException exception) {
			log.warn("로컬도 캐시 직렬화 실패: key={}", key, exception);
		} catch (RuntimeException exception) {
			log.warn("로컬도 캐시 저장 실패: key={}", key, exception);
		}
	}

	public Optional<Map<String, Double>> findAttractions(
		final String regionCode,
		final String districtCode
	) {
		String key = attractionKey(regionCode, districtCode);
		try {
			String value = redisTemplate.opsForValue().get(key);
			if (value == null) {
				return Optional.empty();
			}
			return Optional.of(objectMapper.readValue(value, ATTRACTION_MAP_TYPE));
		} catch (JacksonException exception) {
			redisTemplate.delete(key);
			return Optional.empty();
		} catch (RuntimeException exception) {
			log.warn("관광지 집중률 캐시 조회 실패: key={}", key, exception);
			return Optional.empty();
		}
	}

	public void saveAttractions(
		final String regionCode,
		final String districtCode,
		final Map<String, Double> attractions,
		final Duration ttl
	) {
		String key = attractionKey(regionCode, districtCode);
		try {
			redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(attractions), ttl);
		} catch (JacksonException exception) {
			log.warn("관광지 집중률 캐시 직렬화 실패: key={}", key, exception);
		} catch (RuntimeException exception) {
			log.warn("관광지 집중률 캐시 저장 실패: key={}", key, exception);
		}
	}

	private String key(final String districtCode) {
		return KEY_PREFIX + (districtCode == null || districtCode.isBlank() ? "ALL" : districtCode);
	}

	private String attractionKey(final String regionCode, final String districtCode) {
		return ATTRACTION_KEY_PREFIX
			+ (regionCode == null || regionCode.isBlank() ? "ALL" : regionCode) + ":"
			+ (districtCode == null || districtCode.isBlank() ? "ALL" : districtCode);
	}
}
