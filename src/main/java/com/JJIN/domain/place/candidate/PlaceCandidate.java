package com.JJIN.domain.place.candidate;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;

public record PlaceCandidate(
	Long placeId,
	TourApiContentType contentType,
	String name,
	String address,
	String representativeImageUrl,
	BigDecimal longitude,
	BigDecimal latitude,
	String legalDongRegionCode,
	String legalDongDistrictCode,
	String lclsSystm1Code,
	String lclsSystm2Code,
	LocalDate festivalStartDate,
	LocalDate festivalEndDate,
	Double localityScore,
	ExperienceLevel localityLevel
) {
	public PlaceCandidate withDisplayContent(
		final String displayName,
		final String displayAddress,
		final String displayImageUrl
	) {
		return new PlaceCandidate(
			placeId,
			contentType,
			displayName,
			displayAddress,
			displayImageUrl,
			longitude,
			latitude,
			legalDongRegionCode,
			legalDongDistrictCode,
			lclsSystm1Code,
			lclsSystm2Code,
			festivalStartDate,
			festivalEndDate,
			localityScore,
			localityLevel
		);
	}
}
