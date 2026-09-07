package com.JJIN.domain.place.candidate;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.place.entity.Place;
import com.JJIN.domain.place.entity.PlaceLocalizedContent;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.repository.PlaceLocalizedContentRepository;
import com.JJIN.domain.place.repository.PlaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandidateDisplayService {

	private final PlaceRepository placeRepository;
	private final PlaceLocalizedContentRepository localizedContentRepository;

	@Transactional(readOnly = true)
	public List<PlaceCandidate> localize(
		final Collection<PlaceCandidate> candidates,
		final PlaceLocale displayLocale
	) {
		List<Long> placeIds = candidates.stream().map(PlaceCandidate::placeId).toList();
		Map<Long, PlaceLocalizedContent> contents = displayContents(placeIds, displayLocale);
		return candidates.stream()
			.map(candidate -> withDisplayContent(candidate, contents.get(candidate.placeId())))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<PlaceCandidate> loadFromDatabase(final CandidatePoolQuery query) {
		int limit = Math.multiplyExact(query.pageSize(), query.maxPages());
		List<Place> places = placeRepository.findCandidates(
			query.contentType(),
			normalize(query.legalDongRegionCode()),
			normalize(query.legalDongDistrictCode()),
			normalize(query.lclsSystm1Code()),
			normalize(query.lclsSystm2Code())
		);
		Map<Long, PlaceLocalizedContent> contents = displayContents(
			places.stream().map(Place::getId).toList(),
			query.displayLocale()
		);
		return places.stream()
			.filter(place -> matchesFestivalPeriod(place, query))
			.filter(place -> contents.containsKey(place.getId()))
			.limit(limit)
			.map(place -> toCandidate(place, contents.get(place.getId())))
			.toList();
	}

	private Map<Long, PlaceLocalizedContent> displayContents(
		final Collection<Long> placeIds,
		final PlaceLocale displayLocale
	) {
		Map<Long, PlaceLocalizedContent> result = new LinkedHashMap<>();
		localizedContentRepository.findAllByPlaceIdInAndLocaleAndVisibleTrue(placeIds, displayLocale)
			.forEach(content -> result.put(content.getPlace().getId(), content));
		if (displayLocale != PlaceLocale.KO) {
			localizedContentRepository.findAllByPlaceIdInAndLocaleAndVisibleTrue(placeIds, PlaceLocale.KO)
				.forEach(content -> result.putIfAbsent(content.getPlace().getId(), content));
		}
		localizedContentRepository.findAllByPlaceIdInAndVisibleTrue(placeIds)
			.forEach(content -> result.putIfAbsent(content.getPlace().getId(), content));
		return result;
	}

	private PlaceCandidate withDisplayContent(
		final PlaceCandidate candidate,
		final PlaceLocalizedContent localized
	) {
		if (localized == null) {
			return candidate;
		}
		return candidate.withDisplayContent(
			localized.getName(),
			localized.getAddress(),
			localized.getRepresentativeImageUrl()
		);
	}

	private PlaceCandidate toCandidate(
		final Place place,
		final PlaceLocalizedContent localized
	) {
		return new PlaceCandidate(
			place.getId(),
			place.getContentType(),
			localized.getName(),
			localized.getAddress(),
			localized.getRepresentativeImageUrl(),
			place.getLongitude(),
			place.getLatitude(),
			place.getLegalDongRegionCode(),
			place.getLegalDongDistrictCode(),
			place.getLclsSystm1Code(),
			place.getLclsSystm2Code(),
			place.getFestivalStartDate(),
			place.getFestivalEndDate(),
			place.getLocalityScore(),
			place.getLocalityLevel()
		);
	}

	private boolean matchesFestivalPeriod(final Place place, final CandidatePoolQuery query) {
		if (query.contentType() != com.JJIN.domain.onboarding.entity.enums.TourApiContentType.FESTIVAL_EVENT) {
			return true;
		}
		return place.getFestivalStartDate() != null
			&& place.getFestivalEndDate() != null
			&& !place.getFestivalEndDate().isBefore(query.startDate())
			&& !place.getFestivalStartDate().isAfter(query.endDate());
	}

	private String normalize(final String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
