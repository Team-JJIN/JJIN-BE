package com.JJIN.domain.place.tourapi.query;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;

public record AreaPlaceQuery(
	String legalDongRegionCode,
	String legalDongDistrictCode,
	TourApiContentType contentType,
	String lclsSystm1Code,
	String lclsSystm2Code,
	int page,
	int size
) {
	public AreaPlaceQuery {
		if (contentType == null) {
			throw new IllegalArgumentException("contentType은 필수입니다.");
		}
		if (page < 1 || size < 1) {
			throw new IllegalArgumentException("page와 size는 1 이상이어야 합니다.");
		}
	}

	public AreaPlaceQuery withPage(final int nextPage) {
		return new AreaPlaceQuery(
			legalDongRegionCode,
			legalDongDistrictCode,
			contentType,
			lclsSystm1Code,
			lclsSystm2Code,
			nextPage,
			size
		);
	}
}
