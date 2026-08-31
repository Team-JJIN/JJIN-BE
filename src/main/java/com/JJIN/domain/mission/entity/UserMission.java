package com.JJIN.domain.mission.entity;

import com.JJIN.domain.member.entity.Member;
import com.JJIN.domain.mission.entity.enums.UserMissionStatus;
import com.JJIN.domain.onboarding.entity.TravelPlan;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "user_mission",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_user_mission_plan_mission",
			columnNames = {"travel_plan_id", "mission_id"}
		)
	},
	indexes = {
		@Index(name = "idx_user_mission_member_id", columnList = "member_id"),
		@Index(name = "idx_user_mission_mission_id", columnList = "mission_id"),
		@Index(name = "idx_user_mission_travel_plan_id", columnList = "travel_plan_id")
	}
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserMission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id", nullable = false)
	private Mission mission;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "travel_plan_id", nullable = false)
	private TravelPlan travelPlan;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserMissionStatus status = UserMissionStatus.ADDED;

	@Column(nullable = false, updatable = false)
	private LocalDateTime addedAt;

	@Column
	private LocalDateTime completedAt;

	public static UserMission add(
		final Member member,
		final Mission mission,
		final TravelPlan travelPlan
	) {
		return UserMission.builder()
			.member(member)
			.mission(mission)
			.travelPlan(travelPlan)
			.status(UserMissionStatus.ADDED)
			.build();
	}

	public void complete() {
		this.status = UserMissionStatus.COMPLETED;
		this.completedAt = LocalDateTime.now();
	}

	@PrePersist
	private void prePersist() {
		if (addedAt == null) {
			addedAt = LocalDateTime.now();
		}
	}
}
