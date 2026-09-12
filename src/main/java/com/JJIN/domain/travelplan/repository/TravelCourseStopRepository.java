package com.JJIN.domain.travelplan.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.travelplan.entity.TravelCourseStop;

public interface TravelCourseStopRepository extends JpaRepository<TravelCourseStop, Long> {

	List<TravelCourseStop> findAllByTravelPlanIdAndDayNumberOrderByVisitOrderAsc(
		Long travelPlanId,
		int dayNumber
	);

	@Query("""
		select max(s.visitOrder)
		from TravelCourseStop s
		where s.travelPlan.id = :travelPlanId and s.dayNumber = :dayNumber
		""")
	Optional<Integer> findMaxVisitOrder(
		@Param("travelPlanId") Long travelPlanId,
		@Param("dayNumber") int dayNumber
	);

	@Modifying
	@Query(value = """
		UPDATE travel_course_stop
		SET visit_order = visit_order - 1
		WHERE travel_plan_id = :travelPlanId
		  AND day_number = :dayNumber
		  AND visit_order > :deletedOrder
		ORDER BY visit_order ASC
		""", nativeQuery = true)
	void shiftDownAfter(
		@Param("travelPlanId") Long travelPlanId,
		@Param("dayNumber") int dayNumber,
		@Param("deletedOrder") int deletedOrder
	);
}
