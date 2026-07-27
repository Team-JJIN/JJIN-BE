package com.JJIN.domain.onboarding.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.JJIN.domain.member.entity.Member;
import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.entity.enums.TransportMode;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;
import com.JJIN.global.common.BaseTimeEntity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원의 여행 일정. 온보딩을 정상 진행하면 첫 번째 여행 일정이 생성된다.
 */
@Entity
@Getter
@Table(name = "travel_plan")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelPlan extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "region_id")
	private TravelRegion region;

	@Column(name = "region_undecided", nullable = false)
	private boolean regionUndecided;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "activity_start_time", nullable = false)
	private LocalTime activityStartTime;

	@Column(name = "activity_end_time", nullable = false)
	private LocalTime activityEndTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "transport_mode", nullable = false)
	private TransportMode transportMode;

	@Enumerated(EnumType.STRING)
	@Column(name = "experience_level", nullable = false)
	private ExperienceLevel experienceLevel;

	@OneToMany(
		mappedBy = "travelPlan",
		cascade = CascadeType.ALL,
		orphanRemoval = true,
		fetch = FetchType.LAZY
	)
	private List<TravelPreference> preferences = new ArrayList<>();

	@Builder(access = AccessLevel.PRIVATE)
	private TravelPlan(
		final Member member,
		final TravelRegion region,
		final boolean regionUndecided,
		final LocalDate startDate,
		final LocalDate endDate,
		final LocalTime activityStartTime,
		final LocalTime activityEndTime,
		final TransportMode transportMode,
		final ExperienceLevel experienceLevel
	) {
		this.member = member;
		this.region = region;
		this.regionUndecided = regionUndecided;
		this.startDate = startDate;
		this.endDate = endDate;
		this.activityStartTime = activityStartTime;
		this.activityEndTime = activityEndTime;
		this.transportMode = transportMode;
		this.experienceLevel = experienceLevel;
	}

	public static TravelPlan create(
		final Member member,
		final TravelRegion region,
		final boolean regionUndecided,
		final LocalDate startDate,
		final LocalDate endDate,
		final LocalTime activityStartTime,
		final LocalTime activityEndTime,
		final TransportMode transportMode,
		final ExperienceLevel experienceLevel
	) {
		return TravelPlan.builder()
			.member(member)
			.region(region)
			.regionUndecided(regionUndecided)
			.startDate(startDate)
			.endDate(endDate)
			.activityStartTime(activityStartTime)
			.activityEndTime(activityEndTime)
			.transportMode(transportMode)
			.experienceLevel(experienceLevel)
			.build();
	}

	/**
	 * 세부 취향을 추가하며 양방향 연관관계를 함께 맞춘다.
	 */
	public void addPreference(final TourApiContentType contentType, final TravelSubcategory subcategory) {
		TravelPreference preference = TravelPreference.create(this, contentType, subcategory);
		this.preferences.add(preference);
	}

	public List<TravelPreference> getPreferences() {
		return Collections.unmodifiableList(preferences);
	}
}
