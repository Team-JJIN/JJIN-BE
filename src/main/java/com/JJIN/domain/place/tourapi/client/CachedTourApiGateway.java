package com.JJIN.domain.place.tourapi.client;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.tourapi.cache.TourApiCacheRepository;
import com.JJIN.domain.place.tourapi.config.TourApiProperties;
import com.JJIN.domain.place.tourapi.dto.TourApiIntroItem;
import com.JJIN.domain.place.tourapi.dto.TourApiPage;
import com.JJIN.domain.place.tourapi.dto.TourApiPlaceItem;
import com.JJIN.domain.place.tourapi.query.AreaPlaceQuery;
import com.JJIN.domain.place.tourapi.query.FestivalQuery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachedTourApiGateway {

	private static final String CACHE_PREFIX = "tourapi:";

	private final TourApiClient tourApiClient;
	private final TourApiCacheRepository cacheRepository;
	private final TourApiProperties properties;

	public TourApiPage<TourApiPlaceItem> getAreaPlaces(
		final PlaceLocale locale,
		final AreaPlaceQuery query
	) {
		String key = areaKey(locale, query);
		return findOrFetchPage(key, () -> tourApiClient.getAreaPlaces(locale, query));
	}

	public TourApiPage<TourApiPlaceItem> getFestivals(
		final PlaceLocale locale,
		final FestivalQuery query
	) {
		String key = festivalKey(locale, query);
		return findOrFetchPage(key, () -> tourApiClient.getFestivals(locale, query));
	}

	public Optional<TourApiPlaceItem> getCommonDetail(
		final PlaceLocale locale,
		final String contentId
	) {
		String key = CACHE_PREFIX + "common:" + locale + ":" + contentId;
		Optional<TourApiPlaceItem> cached = safeCacheRead(() -> cacheRepository.findCommonDetail(key));
		if (cached.isPresent()) {
			return cached;
		}

		Optional<TourApiPlaceItem> fetched = tourApiClient.getCommonDetail(locale, contentId);
		fetched.ifPresent(item -> safeCacheWrite(
			() -> cacheRepository.saveCommonDetail(key, item, properties.placeSync().detailCacheTtl())
		));
		return fetched;
	}

	public Optional<TourApiIntroItem> getIntroDetail(
		final PlaceLocale locale,
		final String contentId,
		final TourApiContentType contentType
	) {
		String key = CACHE_PREFIX + "intro:" + locale + ":" + contentId + ":" + contentType;
		Optional<TourApiIntroItem> cached = safeCacheRead(() -> cacheRepository.findIntroDetail(key));
		if (cached.isPresent()) {
			return cached;
		}

		Optional<TourApiIntroItem> fetched = tourApiClient.getIntroDetail(locale, contentId, contentType);
		fetched.ifPresent(item -> safeCacheWrite(
			() -> cacheRepository.saveIntroDetail(key, item, properties.placeSync().detailCacheTtl())
		));
		return fetched;
	}

	private TourApiPage<TourApiPlaceItem> findOrFetchPage(
		final String key,
		final Supplier<TourApiPage<TourApiPlaceItem>> fetcher
	) {
		Optional<TourApiPage<TourApiPlaceItem>> cached = safeCacheRead(
			() -> cacheRepository.findPlacePage(key)
		);
		if (cached.isPresent()) {
			return cached.get();
		}

		TourApiPage<TourApiPlaceItem> fetched = fetcher.get();
		safeCacheWrite(() -> cacheRepository.savePlacePage(key, fetched, properties.placeSync().candidateCacheTtl()));
		return fetched;
	}

	private <T> Optional<T> safeCacheRead(final Supplier<Optional<T>> reader) {
		try {
			return reader.get();
		} catch (RuntimeException exception) {
			log.warn("TourAPI Redis 캐시 조회 실패", exception);
			return Optional.empty();
		}
	}

	private void safeCacheWrite(final Runnable writer) {
		try {
			writer.run();
		} catch (RuntimeException exception) {
			log.warn("TourAPI Redis 캐시 저장 실패", exception);
		}
	}

	private String areaKey(final PlaceLocale locale, final AreaPlaceQuery query) {
		return String.join(":",
			CACHE_PREFIX + "area",
			locale.name(),
			value(query.legalDongRegionCode()),
			value(query.legalDongDistrictCode()),
			query.contentType().name(),
			value(query.lclsSystm1Code()),
			value(query.lclsSystm2Code()),
			String.valueOf(query.page()),
			String.valueOf(query.size())
		);
	}

	private String festivalKey(final PlaceLocale locale, final FestivalQuery query) {
		return String.join(":",
			CACHE_PREFIX + "festival",
			locale.name(),
			value(query.legalDongRegionCode()),
			query.startDate().toString(),
			query.endDate().toString(),
			String.valueOf(query.page()),
			String.valueOf(query.size())
		);
	}

	private String value(final String value) {
		return value == null || value.isBlank() ? "ALL" : value;
	}
}
