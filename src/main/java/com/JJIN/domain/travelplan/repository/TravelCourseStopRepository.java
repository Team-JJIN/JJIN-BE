package com.JJIN.domain.travelplan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.JJIN.domain.travelplan.entity.TravelCourseStop;

public interface TravelCourseStopRepository extends JpaRepository<TravelCourseStop, Long> {

	List<TravelCourseStop> findAllByTravelPlanIdAndDayNumberOrderByVisitOrderAsc(
		Long travelPlanId,
		int dayNumber
	);
}
