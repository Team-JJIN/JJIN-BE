package com.JJIN.domain.travelplan.entity;

import java.time.LocalTime;

import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.place.entity.Place;
import com.JJIN.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 여행 일정 코스의 방문지 한 건
 */
@Entity
@Table(
	name = "travel_course_stop",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_stop_plan_day_order",
		columnNames = {"travel_plan_id", "day_number", "visit_order"}
	),
	indexes = {
		@Index(name = "idx_stop_plan_day", columnList = "travel_plan_id, day_number, visit_order"),
		@Index(name = "idx_stop_place", columnList = "place_id")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelCourseStop extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "travel_plan_id", nullable = false)
	private TravelPlan travelPlan;

	@Column(name = "day_number", nullable = false)
	private int dayNumber;

	@Column(name = "visit_order", nullable = false)
	private int visitOrder;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@Column(name = "planned_start_time")
	private LocalTime plannedStartTime;

	@Column(name = "planned_end_time")
	private LocalTime plannedEndTime;

	@Column(name = "planned_stay_minutes", nullable = false)
	private int plannedStayMinutes;

	@Builder(access = AccessLevel.PRIVATE)
	private TravelCourseStop(
		final TravelPlan travelPlan,
		final int dayNumber,
		final int visitOrder,
		final Place place,
		final LocalTime plannedStartTime,
		final LocalTime plannedEndTime,
		final int plannedStayMinutes
	) {
		this.travelPlan = travelPlan;
		this.dayNumber = dayNumber;
		this.visitOrder = visitOrder;
		this.place = place;
		this.plannedStartTime = plannedStartTime;
		this.plannedEndTime = plannedEndTime;
		this.plannedStayMinutes = plannedStayMinutes;
	}

	public static TravelCourseStop create(
		final TravelPlan travelPlan,
		final int dayNumber,
		final int visitOrder,
		final Place place,
		final LocalTime plannedStartTime,
		final LocalTime plannedEndTime,
		final int plannedStayMinutes
	) {
		return TravelCourseStop.builder()
			.travelPlan(travelPlan)
			.dayNumber(dayNumber)
			.visitOrder(visitOrder)
			.place(place)
			.plannedStartTime(plannedStartTime)
			.plannedEndTime(plannedEndTime)
			.plannedStayMinutes(plannedStayMinutes)
			.build();
	}
}
