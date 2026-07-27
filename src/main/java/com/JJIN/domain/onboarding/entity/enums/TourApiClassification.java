package com.JJIN.domain.onboarding.entity.enums;

/**
 * TourAPI KorService2 신분류체계(lclsSystm) 검색 조건.
 *
 * @param lclsSystm1 대분류 코드
 * @param lclsSystm2 중분류 코드. 대분류 전체를 검색할 때는 null
 * @param lclsSystm3 소분류 코드. 중분류 전체를 검색할 때는 null
 */
public record TourApiClassification(
	String lclsSystm1,
	String lclsSystm2,
	String lclsSystm3
) {

	public TourApiClassification {
		lclsSystm1 = requireNonBlank(lclsSystm1, "lclsSystm1");
		lclsSystm2 = normalize(lclsSystm2);
		lclsSystm3 = normalize(lclsSystm3);

		if (lclsSystm2 == null && lclsSystm3 != null) {
			throw new IllegalArgumentException("lclsSystm3 cannot be set without lclsSystm2");
		}
	}

	public static TourApiClassification of(final String lclsSystm1) {
		return new TourApiClassification(lclsSystm1, null, null);
	}

	public static TourApiClassification of(
		final String lclsSystm1,
		final String lclsSystm2
	) {
		return new TourApiClassification(lclsSystm1, lclsSystm2, null);
	}

	public static TourApiClassification of(
		final String lclsSystm1,
		final String lclsSystm2,
		final String lclsSystm3
	) {
		return new TourApiClassification(lclsSystm1, lclsSystm2, lclsSystm3);
	}

	private static String requireNonBlank(final String value, final String fieldName) {
		String normalized = normalize(value);
		if (normalized == null) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return normalized;
	}

	private static String normalize(final String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
