package com.JJIN.domain.travelplan.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.onboarding.repository.TravelPlanRepository;
import com.JJIN.domain.place.entity.Place;
import com.JJIN.domain.place.entity.PlaceLocalizedContent;
import com.JJIN.domain.place.entity.PlaceOperatingInfo;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.repository.PlaceLocalizedContentRepository;
import com.JJIN.domain.place.repository.PlaceOperatingInfoRepository;
import com.JJIN.domain.place.repository.PlaceRepository;
import com.JJIN.domain.place.schedule.PlaceOpenStatusResolver;
import com.JJIN.domain.travelplan.dto.request.AddCourseStopRequest;
import com.JJIN.domain.travelplan.dto.request.ReorderCourseStopsRequest;
import com.JJIN.domain.travelplan.dto.response.AddCourseStopResponse;
import com.JJIN.domain.travelplan.dto.response.CourseStopResponse;
import com.JJIN.domain.travelplan.dto.response.TravelCourseDayResponse;
import com.JJIN.domain.travelplan.entity.TravelCourseStop;
import com.JJIN.domain.travelplan.exception.TravelPlanErrorCode;
import com.JJIN.domain.travelplan.repository.TravelCourseStopRepository;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 여행 코스(방문지) 조회·편집 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelCourseService {

	private static final double EARTH_RADIUS_METERS = 6_371_000.0;
	private static final int REORDER_OFFSET = 1_000;

	private final TravelPlanRepository travelPlanRepository;
	private final TravelCourseStopRepository courseStopRepository;
	private final PlaceRepository placeRepository;
	private final PlaceLocalizedContentRepository localizedContentRepository;
	private final PlaceOperatingInfoRepository operatingInfoRepository;
	private final PlaceOpenStatusResolver openStatusResolver;
	private final Clock clock;

	/**
	 * 특정 여행 일정의 n일차 코스를 조회한다.
	 * 두 번째 방문지부터는 이전 방문지와의 하버사인 직선거리(m)를 정수로 계산해 함께 반환한다.
	 */
	@Transactional(readOnly = true)
	public TravelCourseDayResponse getCourseDay(
		final Long memberId,
		final Long planId,
		final int dayNumber,
		final PlaceLocale locale
	) {
		TravelPlan plan = travelPlanRepository.findById(planId)
			.orElseThrow(() -> new JjinException(TravelPlanErrorCode.TRAVEL_PLAN_NOT_FOUND));

		if (!plan.getMember().getId().equals(memberId)) {
			throw new JjinException(TravelPlanErrorCode.TRAVEL_PLAN_FORBIDDEN);
		}

		int totalDays = (int) (plan.getEndDate().toEpochDay() - plan.getStartDate().toEpochDay()) + 1;
		if (dayNumber < 1 || dayNumber > totalDays) {
			throw new JjinException(TravelPlanErrorCode.INVALID_DAY_NUMBER);
		}

		List<TravelCourseStop> stops = courseStopRepository
			.findAllByTravelPlanIdAndDayNumberOrderByVisitOrderAsc(planId, dayNumber);

		List<CourseStopResponse> stopResponses = buildStopResponses(stops, locale);

		return new TravelCourseDayResponse(
			plan.getId(),
			plan.getName(),
			dayNumber,
			totalDays,
			plan.getStartDate().plusDays((long) dayNumber - 1),
			stopResponses.size(),
			stopResponses
		);
	}

	/**
	 * 일차 코스의 마지막에 방문지를 추가한다.
	 */
	@Transactional
	public AddCourseStopResponse addStop(
		final Long memberId,
		final Long planId,
		final AddCourseStopRequest request
	) {
		TravelPlan plan = travelPlanRepository.findById(planId)
			.orElseThrow(() -> new JjinException(TravelPlanErrorCode.TRAVEL_PLAN_NOT_FOUND));

		if (!plan.getMember().getId().equals(memberId)) {
			throw new JjinException(TravelPlanErrorCode.TRAVEL_PLAN_FORBIDDEN);
		}

		int dayNumber = request.dayNumber();
		int totalDays = (int) (plan.getEndDate().toEpochDay() - plan.getStartDate().toEpochDay()) + 1;
		if (dayNumber < 1 || dayNumber > totalDays) {
			throw new JjinException(TravelPlanErrorCode.INVALID_DAY_NUMBER);
		}

		Place place = placeRepository.findById(request.placeId())
			.orElseThrow(() -> new JjinException(TravelPlanErrorCode.PLACE_NOT_FOUND));

		int nextOrder = courseStopRepository.findMaxVisitOrder(planId, dayNumber).orElse(0) + 1;

		TravelCourseStop stop = TravelCourseStop.create(plan, dayNumber, nextOrder, place);
		TravelCourseStop saved = courseStopRepository.save(stop);

		return AddCourseStopResponse.of(saved.getId(), dayNumber, nextOrder);
	}

	/**
	 * 코스에서 방문지 하나를 삭제하고, 같은 일차의 뒤 순번들을 한 칸씩 앞으로 당긴다.
	 */
	@Transactional
	public void deleteStop(final Long memberId, final Long planId, final Long stopId) {
		TravelPlan plan = travelPlanRepository.findById(planId)
			.orElseThrow(() -> new JjinException(TravelPlanErrorCode.TRAVEL_PLAN_NOT_FOUND));

		if (!plan.getMember().getId().equals(memberId)) {
			throw new JjinException(TravelPlanErrorCode.TRAVEL_PLAN_FORBIDDEN);
		}

		TravelCourseStop stop = courseStopRepository.findById(stopId)
			.filter(found -> found.getTravelPlan().getId().equals(planId))
			.orElseThrow(() -> new JjinException(TravelPlanErrorCode.COURSE_STOP_NOT_FOUND));

		int dayNumber = stop.getDayNumber();
		int deletedOrder = stop.getVisitOrder();

		courseStopRepository.delete(stop);
		courseStopRepository.flush();
		courseStopRepository.shiftDownAfter(planId, dayNumber, deletedOrder);
	}

	/**
	 * 같은 일차 방문지들의 순번을 입력값으로 일괄 수정한다.
	 */
	@Transactional
	public void reorderStops(
		final Long memberId,
		final Long planId,
		final ReorderCourseStopsRequest request
	) {
		TravelPlan plan = travelPlanRepository.findById(planId)
			.orElseThrow(() -> new JjinException(TravelPlanErrorCode.TRAVEL_PLAN_NOT_FOUND));

		if (!plan.getMember().getId().equals(memberId)) {
			throw new JjinException(TravelPlanErrorCode.TRAVEL_PLAN_FORBIDDEN);
		}

		List<Long> requestedIds = request.orders().stream()
			.map(ReorderCourseStopsRequest.StopOrder::stopId)
			.toList();
		List<TravelCourseStop> stops = courseStopRepository.findAllById(requestedIds);
		if (stops.size() != requestedIds.size()) {
			throw new JjinException(TravelPlanErrorCode.COURSE_STOP_NOT_FOUND);
		}
		if (stops.stream().anyMatch(stop -> !stop.getTravelPlan().getId().equals(planId))) {
			throw new JjinException(TravelPlanErrorCode.COURSE_STOP_NOT_FOUND);
		}

		int dayNumber = stops.get(0).getDayNumber();
		if (stops.stream().anyMatch(stop -> stop.getDayNumber() != dayNumber)) {
			throw new JjinException(TravelPlanErrorCode.INVALID_STOP_ORDER);
		}

		List<TravelCourseStop> allDayStops = courseStopRepository
			.findAllByTravelPlanIdAndDayNumberOrderByVisitOrderAsc(planId, dayNumber);
		if (allDayStops.size() != request.orders().size()) {
			throw new JjinException(TravelPlanErrorCode.INVALID_STOP_ORDER);
		}

		Set<Integer> orders = request.orders().stream()
			.map(ReorderCourseStopsRequest.StopOrder::visitOrder)
			.collect(Collectors.toSet());
		boolean isPermutation = orders.size() == request.orders().size()
			&& orders.stream().min(Integer::compareTo).orElse(0) == 1
			&& orders.stream().max(Integer::compareTo).orElse(0) == request.orders().size();
		if (!isPermutation) {
			throw new JjinException(TravelPlanErrorCode.INVALID_STOP_ORDER);
		}

		courseStopRepository.offsetVisitOrders(planId, dayNumber, REORDER_OFFSET);
		for (ReorderCourseStopsRequest.StopOrder order : request.orders()) {
			courseStopRepository.updateVisitOrder(order.stopId(), order.visitOrder());
		}
	}

	private List<CourseStopResponse> buildStopResponses(
		final List<TravelCourseStop> stops,
		final PlaceLocale locale
	) {
		if (stops.isEmpty()) {
			return List.of();
		}

		List<Long> placeIds = stops.stream().map(stop -> stop.getPlace().getId()).toList();

		Map<Long, PlaceLocalizedContent> localizedByPlaceId =
			loadLocalizedWithFallback(placeIds, locale);

		Map<Long, PlaceOperatingInfo> operatingByPlaceId = operatingInfoRepository
			.findAllByPlaceIdIn(placeIds).stream()
			.collect(Collectors.toMap(
				PlaceOperatingInfo::getPlaceId,
				Function.identity(),
				(a, b) -> a
			));

		LocalDateTime now = LocalDateTime.now(clock);
		List<CourseStopResponse> responses = new ArrayList<>(stops.size());
		Place previous = null;
		for (TravelCourseStop stop : stops) {
			Place place = stop.getPlace();
			PlaceLocalizedContent localized = localizedByPlaceId.get(place.getId());
			PlaceOperatingInfo operating = operatingByPlaceId.get(place.getId());
			Integer distance = previous == null ? null : haversineMeters(previous, place);
			PlaceOpenStatusResolver.DailyOpenInfo openInfo = openStatusResolver.resolve(operating, now);

			responses.add(new CourseStopResponse(
				stop.getId(),
				stop.getVisitOrder(),
				place.getId(),
				localized != null ? localized.getName() : null,
				place.getContentType(),
				localized != null ? localized.getAddress() : null,
				place.getLatitude(),
				place.getLongitude(),
				openInfo.openTime(),
				openInfo.closeTime(),
				openInfo.status(),
				distance
			));
			previous = place;
		}
		return responses;
	}

	/**
	 * 요청 locale → KO → 저장된 다른 언어 순으로 fallback.
	 */
	private Map<Long, PlaceLocalizedContent> loadLocalizedWithFallback(
		final List<Long> placeIds,
		final PlaceLocale locale
	) {
		Map<Long, PlaceLocalizedContent> result = new LinkedHashMap<>();
		localizedContentRepository.findAllByPlaceIdInAndLocaleAndVisibleTrue(placeIds, locale)
			.forEach(content -> result.put(content.getPlace().getId(), content));
		if (locale != PlaceLocale.KO) {
			localizedContentRepository.findAllByPlaceIdInAndLocaleAndVisibleTrue(placeIds, PlaceLocale.KO)
				.forEach(content -> result.putIfAbsent(content.getPlace().getId(), content));
		}
		localizedContentRepository.findAllByPlaceIdInAndVisibleTrue(placeIds)
			.forEach(content -> result.putIfAbsent(content.getPlace().getId(), content));
		return result;
	}

	private Integer haversineMeters(final Place from, final Place to) {
		double lat1 = toRadians(from.getLatitude());
		double lat2 = toRadians(to.getLatitude());
		double dLat = lat2 - lat1;
		double dLon = toRadians(to.getLongitude()) - toRadians(from.getLongitude());

		double sinDLat = Math.sin(dLat / 2);
		double sinDLon = Math.sin(dLon / 2);
		double a = sinDLat * sinDLat + Math.cos(lat1) * Math.cos(lat2) * sinDLon * sinDLon;
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return (int) Math.round(EARTH_RADIUS_METERS * c);
	}

	private double toRadians(final BigDecimal degrees) {
		return Math.toRadians(degrees.doubleValue());
	}
}
