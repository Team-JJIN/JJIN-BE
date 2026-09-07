package com.JJIN.domain.place.tourapi.query;

import java.time.LocalDate;

public record FestivalQuery(
	String legalDongRegionCode,
	LocalDate startDate,
	LocalDate endDate,
	int page,
	int size
) {
	public FestivalQuery {
		if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
			throw new IllegalArgumentException("축제 조회 기간이 올바르지 않습니다.");
		}
		if (page < 1 || size < 1) {
			throw new IllegalArgumentException("page와 size는 1 이상이어야 합니다.");
		}
	}

	public FestivalQuery withPage(final int nextPage) {
		return new FestivalQuery(legalDongRegionCode, startDate, endDate, nextPage, size);
	}
}
