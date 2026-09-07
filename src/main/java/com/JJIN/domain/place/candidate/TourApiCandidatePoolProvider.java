package com.JJIN.domain.place.candidate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.tourapi.client.CachedTourApiGateway;
import com.JJIN.domain.place.tourapi.dto.TourApiPage;
import com.JJIN.domain.place.tourapi.dto.TourApiPlaceItem;
import com.JJIN.domain.place.tourapi.exception.TourApiClientException;
import com.JJIN.domain.place.tourapi.query.AreaPlaceQuery;
import com.JJIN.domain.place.tourapi.query.FestivalQuery;
import com.JJIN.domain.place.tourapi.service.PlaceSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class TourApiCandidatePoolProvider implements CandidatePoolProvider {

	private final CachedTourApiGateway tourApiGateway;
	private final PlaceSyncService placeSyncService;
	private final CandidateDisplayService candidateDisplayService;

	@Override
	public CandidatePool getCandidates(final CandidatePoolQuery query) {
		try {
			List<PlaceCandidate> koreanCandidates = fetchAndSync(PlaceLocale.KO, query);

			if (query.displayLocale() != PlaceLocale.KO) {
				try {
					fetchAndSync(query.displayLocale(), query);
				} catch (TourApiClientException exception) {
					log.warn("TourAPI 번역 데이터 갱신 실패: locale={}", query.displayLocale());
				}
			}

			List<PlaceCandidate> candidates = filterAndDeduplicate(koreanCandidates, query);
			candidates = candidateDisplayService.localize(candidates, query.displayLocale());
			return new CandidatePool(candidates);
		} catch (TourApiClientException exception) {
			List<PlaceCandidate> fallback = candidateDisplayService.loadFromDatabase(query);
			if (fallback.isEmpty()) {
				throw exception;
			}
			log.warn("TourAPI 조회에 실패해 DB에 저장된 후보를 사용합니다.");
			return new CandidatePool(fallback);
		}
	}

	private List<PlaceCandidate> fetchAndSync(
		final PlaceLocale locale,
		final CandidatePoolQuery query
	) {
		List<PlaceCandidate> candidates = new ArrayList<>();

		for (int pageNo = 1; pageNo <= query.maxPages(); pageNo++) {
			TourApiPage<TourApiPlaceItem> page = fetchPage(locale, query, pageNo);
			candidates.addAll(placeSyncService.syncPlaces(locale, page.items()));

			if (!page.hasNext()) {
				break;
			}
		}
		return candidates;
	}

	private TourApiPage<TourApiPlaceItem> fetchPage(
		final PlaceLocale locale,
		final CandidatePoolQuery query,
		final int pageNo
	) {
		if (query.contentType() == TourApiContentType.FESTIVAL_EVENT) {
			return tourApiGateway.getFestivals(locale, new FestivalQuery(
				query.legalDongRegionCode(),
				query.startDate(),
				query.endDate(),
				pageNo,
				query.pageSize()
			));
		}

		return tourApiGateway.getAreaPlaces(locale, new AreaPlaceQuery(
			query.legalDongRegionCode(),
			query.legalDongDistrictCode(),
			query.contentType(),
			query.lclsSystm1Code(),
			query.lclsSystm2Code(),
			pageNo,
			query.pageSize()
		));
	}

	private List<PlaceCandidate> filterAndDeduplicate(
		final List<PlaceCandidate> candidates,
		final CandidatePoolQuery query
	) {
		Map<Long, PlaceCandidate> unique = new LinkedHashMap<>();
		candidates.stream()
			.filter(candidate -> matches(candidate, query))
			.forEach(candidate -> unique.putIfAbsent(candidate.placeId(), candidate));
		return List.copyOf(unique.values());
	}

	private boolean matches(final PlaceCandidate candidate, final CandidatePoolQuery query) {
		if (!equalsIfPresent(query.legalDongDistrictCode(), candidate.legalDongDistrictCode())
			|| !equalsIfPresent(query.lclsSystm1Code(), candidate.lclsSystm1Code())
			|| !equalsIfPresent(query.lclsSystm2Code(), candidate.lclsSystm2Code())) {
			return false;
		}
		if (query.contentType() != TourApiContentType.FESTIVAL_EVENT) {
			return true;
		}
		return candidate.festivalStartDate() != null
			&& candidate.festivalEndDate() != null
			&& !candidate.festivalEndDate().isBefore(query.startDate())
			&& !candidate.festivalStartDate().isAfter(query.endDate());
	}

	private boolean equalsIfPresent(final String expected, final String actual) {
		return expected == null || expected.isBlank() || expected.trim().equals(actual);
	}

}
