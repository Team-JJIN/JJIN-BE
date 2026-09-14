package com.JJIN.domain.recommendation.slot;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.recommendation.dto.TravelProfile;

/**
 * 레벨·활동시간·슬롯 수·선호를 받아 하루치 슬롯 템플릿(빈 자리)을 만든다.
 * 식사 자리는 시각으로 고정하고(점심·저녁), 카페 자리는 최대 1개만 두어 카페-카페 연속을 구조적으로 막는다.
 */
@Component
public class SlotTemplateFactory {

	private static final LocalTime LUNCH = LocalTime.of(12, 30);
	private static final LocalTime DINNER = LocalTime.of(18, 30);
	private static final LocalTime CAFE = LocalTime.of(15, 30);
	// 카페 자리는 하루 방문지가 이 수 이상일 때만 만든다.
	private static final int CAFE_MIN_SLOTS = 5;

	// ACTIVITY 자리의 기본 허용 카테고리(음식점 제외). 선호가 있으면 여기에 더한다.
	private static final Set<TourApiContentType> ACTIVITY_BASE_TYPES = Set.of(
		TourApiContentType.TOURIST_ATTRACTION,
		TourApiContentType.CULTURAL_FACILITY,
		TourApiContentType.SHOPPING);

	private static final Set<TourApiContentType> MEAL_TYPES = Set.of(TourApiContentType.RESTAURANT);

	/**
	 * 하루치 슬롯을 시각 순으로 반환한다.
	 */
	public List<DaySlot> build(final TravelProfile profile, final Set<TourApiContentType> preferredTypes) {
		int slotCount = Math.max(1, profile.slotCount());
		LocalTime start = profile.activityStartTime();
		LocalTime end = profile.activityEndTime();

		List<DaySlot> pinned = new ArrayList<>();
		Set<TourApiContentType> activityTypes = activityTypes(preferredTypes);

		// 식사 자리(활동 시간대에 들어오는 경우만) 고정
		if (within(start, end, LUNCH) && pinned.size() < slotCount) {
			pinned.add(new DaySlot(LUNCH, SlotKind.MEAL, MEAL_TYPES, false, true));
		}
		if (within(start, end, DINNER) && pinned.size() < slotCount) {
			pinned.add(new DaySlot(DINNER, SlotKind.MEAL, MEAL_TYPES, false, true));
		}
		// 카페 자리 1개(슬롯 수가 충분할 때만)
		if (slotCount >= CAFE_MIN_SLOTS && within(start, end, CAFE) && pinned.size() < slotCount) {
			pinned.add(new DaySlot(CAFE, SlotKind.CAFE, MEAL_TYPES, true, true));
		}

		// 남은 자리는 ACTIVITY로 활동 시간대에 고르게 분산.
		// ACTIVITY도 시간민감(timeSensitive=true)으로 두어 방문 시각에 운영 중인 곳만 배치한다.
		// (관광지·공원은 대개 상시개방/미상이라 통과하고, 박물관·궁궐처럼 마감이 있는 곳만 걸러진다.)
		int activityCount = Math.max(0, slotCount - pinned.size());
		List<DaySlot> slots = new ArrayList<>(pinned);
		for (LocalTime time : spread(start, end, activityCount)) {
			slots.add(new DaySlot(time, SlotKind.ACTIVITY, activityTypes, false, true));
		}

		slots.sort(Comparator.comparing(DaySlot::time));
		return slots;
	}

	/** ACTIVITY 허용 카테고리 = 기본(관광·문화·쇼핑) + 선호(음식점 제외) */
	private Set<TourApiContentType> activityTypes(final Set<TourApiContentType> preferredTypes) {
		Set<TourApiContentType> result = new HashSet<>(ACTIVITY_BASE_TYPES);
		if (preferredTypes != null) {
			preferredTypes.stream()
				.filter(type -> type != TourApiContentType.RESTAURANT)
				.forEach(result::add);
		}
		return result;
	}

	/** [start, end] 구간에 count개의 시각을 균등 분산 */
	private List<LocalTime> spread(final LocalTime start, final LocalTime end, final int count) {
		List<LocalTime> times = new ArrayList<>();
		if (count <= 0) {
			return times;
		}
		long total = Duration.between(start, end).toMinutes();
		for (int i = 0; i < count; i++) {
			long offset = Math.round(total * (i + 1.0) / (count + 1.0));
			times.add(start.plusMinutes(offset));
		}
		return times;
	}

	private boolean within(final LocalTime start, final LocalTime end, final LocalTime time) {
		return !time.isBefore(start) && !time.isAfter(end);
	}
}
