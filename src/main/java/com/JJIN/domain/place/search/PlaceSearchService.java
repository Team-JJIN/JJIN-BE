package com.JJIN.domain.place.search;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.JJIN.domain.place.candidate.PlaceCandidate;
import com.JJIN.domain.place.entity.PlaceOperatingInfo;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.exception.PlaceErrorCode;
import com.JJIN.domain.place.repository.PlaceOperatingInfoRepository;
import com.JJIN.domain.place.schedule.PlaceOpenStatusResolver;
import com.JJIN.domain.place.tourapi.client.CachedTourApiGateway;
import com.JJIN.domain.place.tourapi.dto.TourApiPage;
import com.JJIN.domain.place.tourapi.dto.TourApiPlaceItem;
import com.JJIN.domain.place.tourapi.query.KeywordSearchQuery;
import com.JJIN.domain.place.tourapi.service.PlaceSyncService;
import com.JJIN.domain.place.tourapi.service.TourApiPlaceDetailService;
import com.JJIN.domain.travelplan.repository.TravelCourseStopRepository;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TourAPI 키워드 검색. 검색 결과를 Place 테이블에 동기화하고 상세(운영시간)내용을 보강한 뒤
 * placeId·운영상태·사용자 좌표 기준 거리를 포함해 반환한다.
 * 사용자 좌표가 주어지면 가까운 순으로 정렬한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceSearchService {

	private static final double EARTH_RADIUS_METERS = 6_371_000.0;

	private final CachedTourApiGateway tourApiGateway;
	private final PlaceSyncService placeSyncService;
	private final TourApiPlaceDetailService placeDetailService;
	private final PlaceOperatingInfoRepository operatingInfoRepository;
	private final TravelCourseStopRepository courseStopRepository;
	private final PlaceOpenStatusResolver openStatusResolver;
	private final Clock clock;

	public PlaceSearchResponse search(
		final String keyword,
		final PlaceLocale locale,
		final int page,
		final int size,
		final BigDecimal userLatitude,
		final BigDecimal userLongitude,
		final Long planId
	) {
		if (!StringUtils.hasText(keyword)) {
			throw new JjinException(PlaceErrorCode.INVALID_SEARCH_KEYWORD);
		}

		TourApiPage<TourApiPlaceItem> resultPage = tourApiGateway.searchPlaces(
			locale, new KeywordSearchQuery(keyword.trim(), page, size));

		List<PlaceCandidate> synced = placeSyncService.syncPlaces(locale, resultPage.items());
		synced.forEach(candidate -> enrichSafely(candidate.placeId(), locale));

		Map<Long, PlaceOperatingInfo> operatingByPlaceId = operatingInfoRepository
			.findAllByPlaceIdIn(synced.stream().map(PlaceCandidate::placeId).toList()).stream()
			.collect(Collectors.toMap(PlaceOperatingInfo::getPlaceId, Function.identity(), (a, b) -> a));

		Set<Long> addedPlaceIds = planId == null
			? Set.of()
			: Set.copyOf(courseStopRepository.findPlaceIdsByTravelPlanId(planId));

		LocalDateTime now = LocalDateTime.now(clock);
		boolean hasUserLocation = userLatitude != null && userLongitude != null;

		List<PlaceSearchResponse.SearchedPlace> places = synced.stream()
			.map(candidate -> toSearchedPlace(
				candidate, operatingByPlaceId.get(candidate.placeId()),
				now, hasUserLocation, userLatitude, userLongitude,
				addedPlaceIds.contains(candidate.placeId())))
			.sorted(hasUserLocation
				? Comparator.comparing(PlaceSearchResponse.SearchedPlace::distanceMeters,
					Comparator.nullsLast(Comparator.naturalOrder()))
				: Comparator.comparing(p -> 0))
			.toList();

		return new PlaceSearchResponse(resultPage.totalCount(), page, size, places);
	}

	private void enrichSafely(final Long placeId, final PlaceLocale locale) {
		try {
			placeDetailService.enrich(placeId, locale);
		} catch (RuntimeException exception) {
			log.warn("장소 상세 보강 실패, 기본 정보만 반환: placeId={}", placeId, exception);
		}
	}

	private PlaceSearchResponse.SearchedPlace toSearchedPlace(
		final PlaceCandidate candidate,
		final PlaceOperatingInfo operating,
		final LocalDateTime now,
		final boolean hasUserLocation,
		final BigDecimal userLatitude,
		final BigDecimal userLongitude,
		final boolean alreadyAdded
	) {
		PlaceOpenStatusResolver.DailyOpenInfo openInfo = openStatusResolver.resolve(operating, now);
		Integer distance = hasUserLocation
			? haversineMeters(userLatitude, userLongitude, candidate.latitude(), candidate.longitude())
			: null;

		return new PlaceSearchResponse.SearchedPlace(
			candidate.placeId(),
			candidate.contentType(),
			candidate.name(),
			candidate.address(),
			candidate.latitude(),
			candidate.longitude(),
			candidate.representativeImageUrl(),
			openInfo.openTime(),
			openInfo.closeTime(),
			openInfo.status(),
			distance,
			alreadyAdded
		);
	}

	private Integer haversineMeters(
		final BigDecimal lat1Deg,
		final BigDecimal lon1Deg,
		final BigDecimal lat2Deg,
		final BigDecimal lon2Deg
	) {
		double lat1 = Math.toRadians(lat1Deg.doubleValue());
		double lat2 = Math.toRadians(lat2Deg.doubleValue());
		double dLat = lat2 - lat1;
		double dLon = Math.toRadians(lon2Deg.doubleValue()) - Math.toRadians(lon1Deg.doubleValue());

		double sinDLat = Math.sin(dLat / 2);
		double sinDLon = Math.sin(dLon / 2);
		double a = sinDLat * sinDLat + Math.cos(lat1) * Math.cos(lat2) * sinDLon * sinDLon;
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return (int) Math.round(EARTH_RADIUS_METERS * c);
	}
}
