package com.JJIN.domain.place.candidate;

import java.time.LocalDate;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.enums.PlaceLocale;

public record CandidatePoolQuery(
	PlaceLocale displayLocale,
	String legalDongRegionCode,
	String legalDongDistrictCode,
	TourApiContentType contentType,
	String lclsSystm1Code,
	String lclsSystm2Code,
	LocalDate startDate,
	LocalDate endDate,
	int pageSize,
	int maxPages
) {
	public CandidatePoolQuery {
		if (displayLocale == null || contentType == null) {
			throw new IllegalArgumentException("displayLocale과 contentType은 필수입니다.");
		}
		if (pageSize < 1 || maxPages < 1) {
			throw new IllegalArgumentException("pageSize와 maxPages는 1 이상이어야 합니다.");
		}
		if (contentType == TourApiContentType.FESTIVAL_EVENT
			&& (startDate == null || endDate == null || endDate.isBefore(startDate))) {
			throw new IllegalArgumentException("축제 후보 조회 기간이 올바르지 않습니다.");
		}
	}
}
