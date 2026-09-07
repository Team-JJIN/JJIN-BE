package com.JJIN.domain.place.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
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

	/**
	 * 지역(시도) 내에서 사용자 레벨 구간에 맞는 장소 수를 시군구별로 집계한다.
	 * 대표 좌표(avg)는 시군구 인접 판정에 사용한다.
	 */
	@Query("""
		select p.legalDongDistrictCode as districtCode,
		       count(p) as placeCount,
		       avg(p.latitude) as avgLatitude,
		       avg(p.longitude) as avgLongitude
		from Place p
		where p.legalDongRegionCode = :regionCode
		  and p.legalDongDistrictCode is not null
		  and p.contentType in :contentTypes
		  and p.localityLevel = :level
		group by p.legalDongDistrictCode
		""")
	List<DistrictAggregate> aggregateDistrictsByLevel(
		@Param("regionCode") String regionCode,
		@Param("contentTypes") Collection<TourApiContentType> contentTypes,
		@Param("level") ExperienceLevel level
	);

	/**
	 * 선정된 시군구들 안에서 추천 후보 장소를 조회한다.
	 * 축제는 여행 기간과 겹치는 것만 포함하고, 축제가 아닌 장소는 항상 포함한다.
	 */
	@Query("""
		select p
		from Place p
		where p.legalDongRegionCode = :regionCode
		  and p.legalDongDistrictCode in :districtCodes
		  and p.contentType in :contentTypes
		  and (
		    p.contentType <> com.JJIN.domain.onboarding.entity.enums.TourApiContentType.FESTIVAL_EVENT
		    or (p.festivalStartDate <= :tripEnd and p.festivalEndDate >= :tripStart)
		  )
		""")
	List<Place> findRecommendationCandidates(
		@Param("regionCode") String regionCode,
		@Param("districtCodes") Collection<String> districtCodes,
		@Param("contentTypes") Collection<TourApiContentType> contentTypes,
		@Param("tripStart") LocalDate tripStart,
		@Param("tripEnd") LocalDate tripEnd
	);
}
