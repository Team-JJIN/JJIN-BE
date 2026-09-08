package com.JJIN.domain.recommendation.policy;

import java.util.EnumMap;
import java.util.Map;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;

/**
 * 콘텐츠 유형별 예상 체류시간(분).
 */
public final class StayDurationPolicy {

	private static final int DEFAULT_STAY_MINUTES = 60;

	private static final Map<TourApiContentType, Integer> STAY_MINUTES = new EnumMap<>(TourApiContentType.class);

	static {
		STAY_MINUTES.put(TourApiContentType.TOURIST_ATTRACTION, 90);
		STAY_MINUTES.put(TourApiContentType.CULTURAL_FACILITY, 90);
		STAY_MINUTES.put(TourApiContentType.FESTIVAL_EVENT, 120);
		STAY_MINUTES.put(TourApiContentType.LEISURE_SPORTS, 120);
		STAY_MINUTES.put(TourApiContentType.SHOPPING, 60);
		STAY_MINUTES.put(TourApiContentType.RESTAURANT, 60);
		STAY_MINUTES.put(TourApiContentType.TRAVEL_COURSE, 60);
		STAY_MINUTES.put(TourApiContentType.LODGING, 0);
	}

	private StayDurationPolicy() {
	}

	public static int estimatedStayMinutes(final TourApiContentType contentType) {
		return STAY_MINUTES.getOrDefault(contentType, DEFAULT_STAY_MINUTES);
	}
}
