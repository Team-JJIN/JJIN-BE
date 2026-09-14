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

	// 의료관광 세분류 접두어(EX050800 병원·의원·성형외과·피부과 등). 코스 후보에서 제외한다.
	private static final String MEDICAL_LCLS3_PREFIX = "EX0508";

	private final CachedTourApiGateway tourApiGateway;
	private final PlaceSyncService placeSyncService;
	private final CandidateDisplayService candidateDisplayService;

	@Override
	public CandidatePool getCandidates(final CandidatePoolQuery query) {
		// 요청 언어를 주 소스로 사용해 그 언어 TourAPI 서비스에서 직접 후보를 가져온다.
		// (그 언어에 후보가 나오도록 보장. 해당 콘텐츠 유형이 그 언어를 지원하지 않으면 KO로 폴백)
		PlaceLocale sourceLocale = query.displayLocale() != PlaceLocale.KO
			&& query.contentType().supports(query.displayLocale())
			? query.displayLocale() : PlaceLocale.KO;
		try {
			List<PlaceCandidate> sourced = fetchAndSync(sourceLocale, query);

			List<PlaceCandidate> candidates = filterAndDeduplicate(sourced, query);
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
			// 의료(성형외과·피부과·병원 등, lclsSystm3=EX0508xx)는 관광지로 분류돼 있으나 코스 후보에서 제외한다.
			List<TourApiPlaceItem> items = page.items().stream()
				.filter(item -> !isMedical(item))
				.toList();
			candidates.addAll(placeSyncService.syncPlaces(locale, items));

			if (!page.hasNext()) {
				break;
			}
		}
		return candidates;
	}

	/** 의료관광 세분류(EX0508xx: 병원·의원·성형외과·피부과 등) 여부. 온천·스파(EX0501/EX0505)는 제외 대상 아님 */
	private boolean isMedical(final TourApiPlaceItem item) {
		return item.lclsSystm3() != null && item.lclsSystm3().startsWith(MEDICAL_LCLS3_PREFIX);
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
