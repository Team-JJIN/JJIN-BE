package com.JJIN.domain.place.tourapi.query;

/**
 * TourAPI 키워드 검색(searchKeyword2) 요청 조건.
 */
public record KeywordSearchQuery(
	String keyword,
	int page,
	int size
) {
	public KeywordSearchQuery {
		if (keyword == null || keyword.isBlank()) {
			throw new IllegalArgumentException("keyword는 비어 있을 수 없습니다.");
		}
		if (page < 1 || size < 1) {
			throw new IllegalArgumentException("page와 size는 1 이상이어야 합니다.");
		}
	}
}
