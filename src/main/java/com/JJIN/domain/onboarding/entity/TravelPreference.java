package com.JJIN.domain.onboarding.entity;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 여행 일정에 선택된 취향 한 건(TourAPI 관광타입 + 온보딩 세부 취향).
 */
@Entity
@Getter
@Table(
	name = "travel_preference",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_travel_preference_plan_content_type_subcategory",
		columnNames = {"travel_plan_id", "content_type", "subcategory"}
	)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelPreference {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "travel_plan_id", nullable = false)
	private TravelPlan travelPlan;

	@Enumerated(EnumType.STRING)
	@Column(name = "content_type", nullable = false)
	private TourApiContentType contentType;

	@Enumerated(EnumType.STRING)
	@Column(name = "subcategory", nullable = false)
	private TravelSubcategory subcategory;

	private TravelPreference(
		final TravelPlan travelPlan,
		final TourApiContentType contentType,
		final TravelSubcategory subcategory
	) {
		this.travelPlan = travelPlan;
		this.contentType = contentType;
		this.subcategory = subcategory;
	}

	static TravelPreference create(
		final TravelPlan travelPlan,
		final TourApiContentType contentType,
		final TravelSubcategory subcategory
	) {
		return new TravelPreference(travelPlan, contentType, subcategory);
	}
}
