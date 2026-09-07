package com.JJIN.domain.place.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

	List<Place> findAllByContentTypeAndLongitudeAndLatitude(
		TourApiContentType contentType,
		BigDecimal longitude,
		BigDecimal latitude
	);

	@Query("""
		select p
		from Place p
		where p.contentType = :contentType
		  and (:regionCode is null or p.legalDongRegionCode = :regionCode)
		  and (:districtCode is null or p.legalDongDistrictCode = :districtCode)
		  and (:lcls1 is null or p.lclsSystm1Code = :lcls1)
		  and (:lcls2 is null or p.lclsSystm2Code = :lcls2)
		""")
	List<Place> findCandidates(
		@Param("contentType") TourApiContentType contentType,
		@Param("regionCode") String regionCode,
		@Param("districtCode") String districtCode,
		@Param("lcls1") String lcls1,
		@Param("lcls2") String lcls2
	);
}
