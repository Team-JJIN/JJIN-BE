package com.JJIN.domain.recommendation.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.candidate.CandidatePool;
import com.JJIN.domain.place.candidate.CandidatePoolProvider;
import com.JJIN.domain.place.candidate.CandidatePoolQuery;
import com.JJIN.domain.place.candidate.PlaceCandidate;
import com.JJIN.domain.place.entity.enums.PlaceLocale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * (시군구 × 콘텐츠 유형) 한 조합의 후보를 독립 트랜잭션으로 조회·동기화한다.
 * 후보 수집을 병렬화할 때 각 조합이 자기 트랜잭션(REQUIRES_NEW)에서 커밋되도록 분리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateFetchWorker {

	private final CandidatePoolProvider candidatePoolProvider;

	/**
	 * 한 조합의 후보 placeId 목록을 반환한다. 조회 실패는 빈 목록으로 격리한다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public List<Long> fetchPlaceIds(
		final String regionCode,
		final String districtCode,
		final TourApiContentType contentType,
		final LocalDate tripStart,
		final LocalDate tripEnd,
		final PlaceLocale locale,
		final int pageSize,
		final int maxPages
	) {
		try {
			CandidatePoolQuery query = new CandidatePoolQuery(
				locale, regionCode, districtCode, contentType,
				null, null, tripStart, tripEnd, pageSize, maxPages);
			CandidatePool pool = candidatePoolProvider.getCandidates(query);
			return pool.candidates().stream().map(PlaceCandidate::placeId).toList();
		} catch (RuntimeException exception) {
			log.warn("후보 조회 실패, 건너뜀: region={}, district={}, type={}, msg={}",
				regionCode, districtCode, contentType, exception.getMessage());
			return List.of();
		}
	}
}
