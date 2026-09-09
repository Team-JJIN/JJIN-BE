package com.JJIN.domain.travelplan.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.onboarding.repository.TravelPlanRepository;
import com.JJIN.domain.place.entity.Place;
import com.JJIN.domain.place.entity.PlaceLocalizedContent;
import com.JJIN.domain.place.entity.PlaceOperatingInfo;
import com.JJIN.domain.place.entity.enums.OpenStatus;
import com.JJIN.domain.place.entity.enums.OperatingInfoParseStatus;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.repository.PlaceLocalizedContentRepository;
import com.JJIN.domain.place.repository.PlaceOperatingInfoRepository;
import com.JJIN.domain.place.schedule.OpenStatusCalculator;
import com.JJIN.domain.place.schedule.WeeklySchedule;
import com.JJIN.domain.travelplan.dto.response.CourseStopResponse;
import com.JJIN.domain.travelplan.dto.response.TravelCourseDayResponse;
import com.JJIN.domain.travelplan.entity.TravelCourseStop;
import com.JJIN.domain.travelplan.exception.TravelPlanErrorCode;
import com.JJIN.domain.travelplan.repository.TravelCourseStopRepository;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 여행 코스(방문지) 조회·편집 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelCourseService {

	private static final double EARTH_RADIUS_METERS = 6_371_000.0;

	private final TravelPlanRepository travelPlanRepository;
	private final TravelCourseStopRepository courseStopRepository;
	private final PlaceLocalizedContentRepository localizedContentRepository;
	private final PlaceOperatingInfoRepository operatingInfoRepository;
	private final OpenStatusCalculator openStatusCalculator;
	private final ObjectMapper objectMapper;
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
			OpenStatus openStatus = computeOpenStatus(operating, now);

			responses.add(new CourseStopResponse(
				stop.getId(),
				stop.getVisitOrder(),
				place.getId(),
				localized != null ? localized.getName() : null,
				place.getContentType(),
				localized != null ? localized.getAddress() : null,
				place.getLatitude(),
				place.getLongitude(),
				operating != null ? operating.getRawOpeningHoursText() : null,
				openStatus,
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

	private OpenStatus computeOpenStatus(final PlaceOperatingInfo operating, final LocalDateTime now) {
		if (operating == null
			|| operating.getParseStatus() == OperatingInfoParseStatus.NOT_PARSED
			|| operating.getParseStatus() == OperatingInfoParseStatus.FAILED
			|| operating.getWeeklyScheduleJson() == null) {
			return OpenStatus.UNKNOWN;
		}
		try {
			WeeklySchedule schedule = objectMapper.readValue(
				operating.getWeeklyScheduleJson(), WeeklySchedule.class);
			return openStatusCalculator.compute(schedule, now);
		} catch (JacksonException exception) {
			log.warn("weeklyScheduleJson 역직렬화 실패: placeId={}", operating.getPlaceId(), exception);
			return OpenStatus.UNKNOWN;
		}
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
