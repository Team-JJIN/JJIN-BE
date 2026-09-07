package com.JJIN.domain.recommendation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.onboarding.entity.TravelRegion;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.Place;
import com.JJIN.domain.place.repository.DistrictAggregate;
import com.JJIN.domain.place.repository.PlaceRepository;
import com.JJIN.domain.recommendation.dto.TravelProfile;
import com.JJIN.global.geo.GeoPoint;
import com.JJIN.global.geo.GeoUtils;

import lombok.RequiredArgsConstructor;

/**
 * 후보 풀 수집.
 * 사용자 레벨에 맞는 시군구를 선정하고, 선정된 시군구에서 추천 후보 장소를 DB에서 조회한다.
 */
@Service
@RequiredArgsConstructor
public class CandidatePoolService {

	private final PlaceRepository placeRepository;

	/**
	 * 사용자 레벨 구간 장소 수가 가장 많은 시군구(앵커)와, 앵커에 인접한 시군구들을
	 * 프로파일의 필요 시군구 수만큼 선정한다.
	 */
	@Transactional(readOnly = true)
	public List<String> selectDistricts(
		final TravelProfile profile,
		final TravelRegion region,
		final Set<TourApiContentType> contentTypes
	) {
		if (contentTypes.isEmpty()) {
			return List.of();
		}

		// 지역 내에서 사용자 레벨 구간에 맞는 장소 수를 시군구별로 집계
		List<DistrictAggregate> aggregates = placeRepository.aggregateDistrictsByLevel(
			region.getLDongRegnCd(), contentTypes, profile.experienceLevel());
		if (aggregates.isEmpty()) {
			return List.of();
		}

		// 레벨 구간 장소 수가 가장 많은 시군구를 앵커로 선정
		DistrictAggregate anchor = aggregates.stream()
			.max(Comparator.comparingLong(DistrictAggregate::getPlaceCount))
			.orElseThrow();
		GeoPoint anchorCentroid = new GeoPoint(anchor.getAvgLatitude(), anchor.getAvgLongitude());

		List<String> selected = new ArrayList<>();
		selected.add(anchor.getDistrictCode());

		// 앵커에 가까운 시군구부터 필요 수(n-1)만큼 추가
		aggregates.stream()
			.filter(aggregate -> !aggregate.getDistrictCode().equals(anchor.getDistrictCode()))
			.sorted(Comparator.comparingDouble(aggregate -> GeoUtils.haversineKm(
				anchorCentroid, new GeoPoint(aggregate.getAvgLatitude(), aggregate.getAvgLongitude()))))
			.limit(Math.max(0, profile.requiredDistrictCount() - 1))
			.forEach(aggregate -> selected.add(aggregate.getDistrictCode()));

		return selected;
	}

	/**
	 * 선정된 시군구에서 추천 후보 장소를 조회한다. 축제는 여행 기간과 겹치는 것만 포함된다.
	 */
	@Transactional(readOnly = true)
	public List<Place> collectCandidates(
		final TravelRegion region,
		final List<String> districtCodes,
		final Set<TourApiContentType> contentTypes,
		final LocalDate tripStart,
		final LocalDate tripEnd
	) {
		if (districtCodes.isEmpty() || contentTypes.isEmpty()) {
			return List.of();
		}
		return placeRepository.findRecommendationCandidates(
			region.getLDongRegnCd(), districtCodes, contentTypes, tripStart, tripEnd);
	}
}
