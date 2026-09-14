package com.JJIN.domain.recommendation.service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.tourapi.service.TourApiPlaceDetailService;

import lombok.extern.slf4j.Slf4j;

/**
 * 후보 장소들의 운영시간(TourAPI 상세)을 병렬로 조회·저장한다.
 * 조회 실패는 개별적으로 격리하며, 결과는 DB·캐시에 남아 재사용된다.
 */
@Slf4j
@Component
public class OperatingInfoEnricher {

	private final TourApiPlaceDetailService placeDetailService;
	private final Executor enrichExecutor;

	public OperatingInfoEnricher(
		final TourApiPlaceDetailService placeDetailService,
		@Qualifier("candidateFetchExecutor") final Executor enrichExecutor
	) {
		this.placeDetailService = placeDetailService;
		this.enrichExecutor = enrichExecutor;
	}

	/**
	 * 주어진 placeId들의 운영시간을 병렬 조회·저장한다. 이미 채워진 곳은 캐시 히트로 저렴하다.
	 */
	public void enrichAll(final Collection<Long> placeIds, final PlaceLocale locale) {
		if (placeIds == null || placeIds.isEmpty()) {
			return;
		}
		List<CompletableFuture<Void>> futures = placeIds.stream()
			.distinct()
			.map(placeId -> CompletableFuture.runAsync(() -> enrichSafely(placeId, locale), enrichExecutor))
			.toList();
		futures.forEach(CompletableFuture::join);
	}

	private void enrichSafely(final Long placeId, final PlaceLocale locale) {
		try {
			placeDetailService.enrich(placeId, locale);
		} catch (RuntimeException exception) {
			log.warn("운영시간 enrich 실패, 건너뜀: placeId={}, msg={}", placeId, exception.getMessage());
		}
	}
}
