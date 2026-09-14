package com.JJIN.domain.recommendation.locality;

import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.JJIN.global.geo.GeoPoint;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 전국 시군구 대표 좌표를 정적 리소스(geo/district-centroids.json)에서 로드해 제공한다.
 * 키는 방문자수/집중률 API의 5자리 signguCode와 동일하다.
 * 시군구 선정 시 좌표 소스로 사용하며, DB 적재와 무관해 cold-start에서도 동작한다.
 */
@Slf4j
@Component
public class DistrictCentroidProvider {

	private static final String RESOURCE_PATH = "geo/district-centroids.json";

	private final ObjectMapper objectMapper;
	private Map<String, GeoPoint> centroidBySigngu = Map.of();

	public DistrictCentroidProvider(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	void load() {
		try {
			Map<String, RawCentroid> raw = objectMapper.readValue(
				new ClassPathResource(RESOURCE_PATH).getInputStream(),
				new TypeReference<Map<String, RawCentroid>>() {
				});
			centroidBySigngu = raw.entrySet().stream()
				.collect(java.util.stream.Collectors.toUnmodifiableMap(
					Map.Entry::getKey,
					e -> new GeoPoint(e.getValue().lat(), e.getValue().lng())));
			log.info("시군구 대표 좌표 로드 완료: {}개", centroidBySigngu.size());
		} catch (Exception exception) {
			log.error("시군구 대표 좌표 로드 실패: {}", RESOURCE_PATH, exception);
			centroidBySigngu = Map.of();
		}
	}

	/** 5자리 signguCode(regionCode 2 + district 3)의 중심 좌표. 없으면 null. */
	public GeoPoint centroid(final String signguCode) {
		return centroidBySigngu.get(signguCode);
	}

	private record RawCentroid(double lat, double lng) {
	}
}
