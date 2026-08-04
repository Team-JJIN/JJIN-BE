package com.JJIN.domain.mission.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "mission_tag_mapping",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_mission_tag_mapping_mission_tag",
			columnNames = {"mission_id", "tag_id"}
		)
	},
	indexes = {
		@Index(name = "idx_mission_tag_mapping_mission_id", columnList = "mission_id"),
		@Index(name = "idx_mission_tag_mapping_tag_id", columnList = "tag_id")
	}
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MissionTagMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id", nullable = false)
	private Mission mission;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tag_id", nullable = false)
	private MissionTag tag;

	public static MissionTagMapping of(final Mission mission, final MissionTag tag) {
		return MissionTagMapping.builder()
			.mission(mission)
			.tag(tag)
			.build();
	}
}
