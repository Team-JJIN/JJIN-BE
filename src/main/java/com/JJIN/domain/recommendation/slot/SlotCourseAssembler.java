package com.JJIN.domain.recommendation.slot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.PlaceOperatingInfo;
import com.JJIN.domain.place.entity.enums.OpenStatus;
import com.JJIN.domain.place.repository.PlaceOperatingInfoRepository;
import com.JJIN.domain.place.schedule.OpenStatusCalculator;
import com.JJIN.domain.place.schedule.WeeklySchedule;
import com.JJIN.domain.recommendation.dto.CourseDraft;
import com.JJIN.domain.recommendation.dto.CourseDraft.DayPlan;
import com.JJIN.domain.recommendation.dto.CourseDraft.PlannedVisit;
import com.JJIN.domain.recommendation.dto.RecommendationCandidate;
import com.JJIN.domain.recommendation.dto.ScoredCandidate;
import com.JJIN.domain.recommendation.dto.TravelProfile;
import com.JJIN.domain.recommendation.service.TravelTimeEstimator;
import com.JJIN.global.geo.GeoPoint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * 슬롯 기반 코스 조립(P4). LLM 대신 결정론적 알고리즘으로 코스를 짠다.
 * 하루를 빈 슬롯(시각·허용 카테고리)으로 먼저 만들고, 각 슬롯을 그리디로 채운다.
 * - 식사 슬롯 = 음식점 고정(밥 시간엔 음식점), 카페 슬롯 1개 → 카페-카페 구조적 차단
 * - 같은 중분류(lclsSystm2) 연속 배치 회피, 직전 방문지에서 가까운(동선) 곳 우선, 운영 중인 곳 우선
 * - 이동시간은 직선거리(haversine) × 이동수단 속도 근사로 추정해 동선을 최적화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotCourseAssembler {

	private static final int DEFAULT_STAY_MINUTES = 90;
	private static final String CAFE_LCLS2 = "FD05";
	// 점수에서 이동시간(분)당 빼는 패널티. 점수(0~100)와 동선(수십 분)의 균형을 잡는다.
	private static final double DISTANCE_PENALTY_PER_MINUTE = 0.6;
	// 슬롯을 채울 때 조정점수 상위 몇 개 중에서 무작위로 고를지(매 생성마다 코스 다양화).
	private static final int RANDOM_TOP_POOL = 5;

	private final SlotTemplateFactory slotTemplateFactory;
	private final TravelTimeEstimator travelTimeEstimator;
	private final OpenStatusCalculator openStatusCalculator;
	private final PlaceOperatingInfoRepository operatingInfoRepository;
	private final ObjectMapper objectMapper;

	/**
	 * 일자 배정(dayByPlaceId)과 슬롯 템플릿에 따라 코스를 조립한다.
	 */
	public CourseDraft assemble(
		final List<ScoredCandidate> scored,
		final Map<Long, Integer> dayByPlaceId,
		final TravelProfile profile,
		final LocalDate tripStart,
		final Set<TourApiContentType> preferredTypes
	) {
		if (scored == null || scored.isEmpty()) {
			return null;
		}

		// activity 슬롯에서 우선 채울 선호 카테고리(음식점 제외). 비어 있으면 선호 우선 없음.
		Set<TourApiContentType> preferredActivity = new HashSet<>();
		if (preferredTypes != null) {
			preferredTypes.stream()
				.filter(type -> type != TourApiContentType.RESTAURANT)
				.forEach(preferredActivity::add);
		}

		Set<Long> usedPlaceIds = new HashSet<>();
		List<DayPlan> days = new ArrayList<>();

		for (int day = 1; day <= profile.tripDays(); day++) {
			final int currentDay = day;
			List<ScoredCandidate> dayCandidates = scored.stream()
				.filter(s -> dayByPlaceId.getOrDefault(s.candidate().placeId(), 1) == currentDay)
				.toList();

			List<DaySlot> slots = slotTemplateFactory.build(profile, preferredTypes);
			LocalDate date = tripStart.plusDays(day - 1L);
			List<PlannedVisit> visits = fillDay(slots, dayCandidates, date, usedPlaceIds, profile, preferredActivity);
			days.add(new DayPlan(day, visits));
		}
		return new CourseDraft(days);
	}

	private List<PlannedVisit> fillDay(
		final List<DaySlot> slots,
		final List<ScoredCandidate> dayCandidates,
		final LocalDate date,
		final Set<Long> usedPlaceIds,
		final TravelProfile profile,
		final Set<TourApiContentType> preferredActivity
	) {
		List<PlannedVisit> visits = new ArrayList<>();
		String previousMid = null;
		GeoPoint previousLocation = null;

		for (DaySlot slot : slots) {
			ScoredCandidate pick = select(
				slot, dayCandidates, usedPlaceIds, previousMid, previousLocation, date, profile, preferredActivity);
			if (pick == null) {
				continue;
			}
			RecommendationCandidate c = pick.candidate();
			usedPlaceIds.add(c.placeId());

			int stay = c.estimatedStayMinutes() > 0 ? c.estimatedStayMinutes() : DEFAULT_STAY_MINUTES;
			LocalTime startTime = slot.time();
			LocalTime endTime = startTime.plusMinutes(stay);
			if (endTime.isAfter(profile.activityEndTime())) {
				endTime = profile.activityEndTime();
			}
			visits.add(new PlannedVisit(c.placeId(), startTime.toString(), endTime.toString(), stay));

			previousMid = c.lclsSystm2Code();
			previousLocation = c.location();
		}
		return visits;
	}

	/**
	 * 슬롯 하나를 채울 후보를 고른다. 제약(카테고리 정제·운영중·중분류 비연속)을 우선순위대로 완화하며 탐색한다.
	 * 완화 우선순위(오래 지킬수록 중요): 운영중 > 카테고리 정제 > 중분류 비연속.
	 * 카테고리 정제 = 카페 슬롯은 카페(FD05), 식사 슬롯은 카페 아닌 음식점, activity 슬롯은 선호 카테고리.
	 */
	private ScoredCandidate select(
		final DaySlot slot,
		final List<ScoredCandidate> dayCandidates,
		final Set<Long> usedPlaceIds,
		final String previousMid,
		final GeoPoint previousLocation,
		final LocalDate date,
		final TravelProfile profile,
		final Set<TourApiContentType> preferredActivity
	) {
		List<ScoredCandidate> base = dayCandidates.stream()
			.filter(s -> slot.allowedTypes().contains(s.candidate().contentType()))
			.filter(s -> !usedPlaceIds.contains(s.candidate().placeId()))
			.toList();
		if (base.isEmpty()) {
			return null;
		}

		// (운영중, 카테고리 정제, 중분류비연속) 제약을 점차 완화하는 순서
		boolean[][] relaxations = {
			{true, true, true},
			{true, true, false},
			{true, false, false},
			{false, false, false}
		};
		boolean refineApplies = slot.kind() == SlotKind.CAFE || slot.kind() == SlotKind.MEAL
			|| (slot.kind() == SlotKind.ACTIVITY && !preferredActivity.isEmpty());
		for (boolean[] relax : relaxations) {
			boolean useOpen = relax[0] && slot.timeSensitive();
			boolean useRefine = relax[1] && refineApplies;
			boolean useMid = relax[2] && previousMid != null;

			List<ScoredCandidate> pool = base.stream()
				.filter(s -> !useRefine || matchesRefine(slot, s.candidate(), preferredActivity))
				.filter(s -> !useMid || !Objects.equals(previousMid, s.candidate().lclsSystm2Code()))
				.filter(s -> !useOpen || isOpenAt(s.candidate().placeId(), date, slot.time()))
				.toList();
			if (!pool.isEmpty()) {
				return pickBest(pool, previousLocation, profile);
			}
		}
		return pickBest(base, previousLocation, profile);
	}

	/**
	 * 카페 슬롯은 카페(FD05), 식사 슬롯은 카페 아닌 음식점, activity 슬롯은 선호 카테고리를 우선한다.
	 */
	private boolean matchesRefine(
		final DaySlot slot,
		final RecommendationCandidate candidate,
		final Set<TourApiContentType> preferredActivity
	) {
		boolean isCafe = CAFE_LCLS2.equals(candidate.lclsSystm2Code());
		return switch (slot.kind()) {
			case CAFE -> isCafe;
			case MEAL -> !isCafe;
			case ACTIVITY -> preferredActivity.contains(candidate.contentType());
		};
	}

	/**
	 * 점수에서 이동시간 패널티를 뺀 조정점수 상위 {@link #RANDOM_TOP_POOL}개 중에서 조정점수 가중 무작위로 고른다.
	 * 상위권 안에서만 흔들어 품질(선호·동선·운영시간)은 지키면서 매 생성마다 코스가 달라지게 한다.
	 */
	private ScoredCandidate pickBest(
		final List<ScoredCandidate> pool,
		final GeoPoint previousLocation,
		final TravelProfile profile
	) {
		if (pool.isEmpty()) {
			return null;
		}
		// 조정점수 = 최종점수 - 이동시간 패널티
		List<ScoredValue> ranked = pool.stream()
			.map(s -> new ScoredValue(s, adjustedValue(s, previousLocation, profile)))
			.sorted(Comparator.comparingDouble(ScoredValue::value).reversed())
			.limit(RANDOM_TOP_POOL)
			.toList();

		// 음수 조정점수도 다룰 수 있게 최솟값 기준으로 이동시켜 가중치를 양수화
		double minValue = ranked.get(ranked.size() - 1).value();
		double totalWeight = 0.0;
		for (ScoredValue sv : ranked) {
			totalWeight += sv.value() - minValue + 1.0;
		}
		double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
		double cumulative = 0.0;
		for (ScoredValue sv : ranked) {
			cumulative += sv.value() - minValue + 1.0;
			if (roll < cumulative) {
				return sv.candidate();
			}
		}
		return ranked.get(0).candidate();
	}

	private double adjustedValue(
		final ScoredCandidate candidate,
		final GeoPoint previousLocation,
		final TravelProfile profile
	) {
		double travelMinutes = previousLocation == null ? 0
			: travelTimeEstimator.estimateMinutes(
				previousLocation, candidate.candidate().location(), profile.transportMode());
		return candidate.finalScore() - DISTANCE_PENALTY_PER_MINUTE * travelMinutes;
	}

	private record ScoredValue(ScoredCandidate candidate, double value) {
	}

	/** 운영정보(enrich 후 최신)를 읽어 해당 시각에 운영 중인지 판단. 미상이면 허용한다. */
	private boolean isOpenAt(final Long placeId, final LocalDate date, final LocalTime time) {
		Optional<PlaceOperatingInfo> info = operatingInfoRepository.findById(placeId);
		if (info.isEmpty()) {
			return true;
		}
		String json = info.get().getWeeklyScheduleJson();
		if (json == null || json.isBlank()) {
			return true;
		}
		WeeklySchedule schedule;
		try {
			schedule = objectMapper.readValue(json, WeeklySchedule.class);
		} catch (RuntimeException exception) {
			return true;
		}
		OpenStatus status = openStatusCalculator.compute(schedule, LocalDateTime.of(date, time));
		return status != OpenStatus.CLOSED && status != OpenStatus.BREAK;
	}
}
