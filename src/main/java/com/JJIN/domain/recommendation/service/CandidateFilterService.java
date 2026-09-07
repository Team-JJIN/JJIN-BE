package com.JJIN.domain.recommendation.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.JJIN.domain.recommendation.dto.PlaceCandidate;
import com.JJIN.domain.recommendation.dto.TravelProfile;
import com.JJIN.global.geo.GeoPoint;

import lombok.RequiredArgsConstructor;

/**
 * 하드 필터. 규칙을 위반하는 후보를 후보 풀에서 제거한다.
 */
@Service
@RequiredArgsConstructor
public class CandidateFilterService {

	// 대한민국 좌표 범위(근사)
	private static final double KOREA_LAT_MIN = 33.0;
	private static final double KOREA_LAT_MAX = 38.7;
	private static final double KOREA_LNG_MIN = 124.5;
	private static final double KOREA_LNG_MAX = 131.9;

	private final OpenStatusCalculator openStatusCalculator;

	public List<PlaceCandidate> applyHardFilters(
		final List<PlaceCandidate> candidates,
		final TravelProfile profile,
		final LocalDate tripStart,
		final LocalDate tripEnd
	) {
		return candidates.stream()
			.filter(this::hasValidCoordinates)
			.filter(candidate -> festivalDateMatches(candidate, tripStart, tripEnd))
			.filter(candidate -> operatingOverlapsActivity(candidate, profile, tripStart, tripEnd))
			.toList();
	}

	/** 좌표가 존재하고 대한민국 범위 안에 있는가 */
	private boolean hasValidCoordinates(final PlaceCandidate candidate) {
		GeoPoint location = candidate.location();
		if (location == null) {
			return false;
		}
		return location.latitude() >= KOREA_LAT_MIN && location.latitude() <= KOREA_LAT_MAX
			&& location.longitude() >= KOREA_LNG_MIN && location.longitude() <= KOREA_LNG_MAX;
	}

	/** 축제라면 행사 기간이 여행 기간과 겹치는가 (축제가 아니면 통과) */
	private boolean festivalDateMatches(final PlaceCandidate candidate, final LocalDate tripStart, final LocalDate tripEnd) {
		if (!candidate.isFestival()) {
			return true;
		}
		LocalDate start = candidate.festivalStartDate();
		LocalDate end = candidate.festivalEndDate();
		return start != null && end != null && !start.isAfter(tripEnd) && !end.isBefore(tripStart);
	}

	/** 여행 기간 중 하루라도 운영시간이 활동 시간대와 겹치는가 (운영시간 미상은 제외되지 않음) */
	private boolean operatingOverlapsActivity(
		final PlaceCandidate candidate,
		final TravelProfile profile,
		final LocalDate tripStart,
		final LocalDate tripEnd
	) {
		for (LocalDate date = tripStart; !date.isAfter(tripEnd); date = date.plusDays(1)) {
			boolean overlaps = openStatusCalculator.overlapsActivityWindow(
				candidate.weeklySchedule(), date.getDayOfWeek(),
				profile.activityStartTime(), profile.activityEndTime());
			if (overlaps) {
				return true;
			}
		}
		return false;
	}
}
