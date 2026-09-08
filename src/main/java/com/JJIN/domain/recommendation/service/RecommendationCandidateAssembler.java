package com.JJIN.domain.recommendation.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.place.dto.WeeklySchedule;
import com.JJIN.domain.place.entity.Place;
import com.JJIN.domain.place.entity.PlaceOperatingInfo;
import com.JJIN.domain.place.entity.enums.OperatingInfoParseStatus;
import com.JJIN.domain.place.repository.PlaceOperatingInfoRepository;
import com.JJIN.domain.recommendation.dto.RecommendationCandidate;
import com.JJIN.domain.recommendation.locality.service.LocalityScoreCalculator;
import com.JJIN.domain.recommendation.policy.LocalityPolicy;
import com.JJIN.domain.recommendation.policy.StayDurationPolicy;
import com.JJIN.global.geo.GeoPoint;

import lombok.RequiredArgsConstructor;

/**
 * Place 목록에 운영정보를 결합해 추천용 RecommendationCandidate로 조립한다.
 * 운영시간 원문(weeklyScheduleJson)은 이 시점에 파싱하고, 예상 체류시간은 유형 상수로 채운다.
 */
@Service
@RequiredArgsConstructor
public class RecommendationCandidateAssembler {

	private final PlaceOperatingInfoRepository operatingInfoRepository;
	private final WeeklyScheduleParser weeklyScheduleParser;
	private final LocalityScoreCalculator localityScoreCalculator;

	public List<RecommendationCandidate> assemble(final List<Place> places) {
		if (places.isEmpty()) {
			return List.of();
		}

		List<Long> placeIds = places.stream().map(Place::getId).toList();
		Map<Long, PlaceOperatingInfo> operatingInfoByPlaceId =
			operatingInfoRepository.findAllByPlaceIdIn(placeIds).stream()
				.collect(Collectors.toMap(PlaceOperatingInfo::getPlaceId, Function.identity()));

		List<Place> needsLocality = places.stream()
			.filter(place -> place.getLocalityScore() == null)
			.toList();
		Map<Long, Double> resolvedLocality = needsLocality.isEmpty()
			? Map.of()
			: localityScoreCalculator.resolveScores(needsLocality);

		return places.stream()
			.map(place -> toCandidate(
				place, operatingInfoByPlaceId.get(place.getId()), resolvedLocality.get(place.getId())))
			.toList();
	}

	private RecommendationCandidate toCandidate(
		final Place place,
		final PlaceOperatingInfo operatingInfo,
		final Double resolvedLocality
	) {
		WeeklySchedule weeklySchedule = operatingInfo == null
			? null : weeklyScheduleParser.parse(operatingInfo.getWeeklyScheduleJson());
		OperatingInfoParseStatus parseStatus = operatingInfo == null
			? OperatingInfoParseStatus.NOT_PARSED : operatingInfo.getParseStatus();

		Double localityScore = place.getLocalityScore() != null
			? place.getLocalityScore() : resolvedLocality;
		ExperienceLevel localityLevel = place.getLocalityLevel() != null
			? place.getLocalityLevel()
			: (localityScore == null ? null : LocalityPolicy.classify(localityScore));

		return new RecommendationCandidate(
			place.getId(),
			place.getContentType(),
			place.getLclsSystm1Code(),
			place.getLclsSystm2Code(),
			GeoPoint.of(place.getLatitude(), place.getLongitude()),
			localityScore,
			localityLevel,
			parseStatus,
			weeklySchedule,
			place.getFestivalStartDate(),
			place.getFestivalEndDate(),
			StayDurationPolicy.estimatedStayMinutes(place.getContentType())
		);
	}
}
