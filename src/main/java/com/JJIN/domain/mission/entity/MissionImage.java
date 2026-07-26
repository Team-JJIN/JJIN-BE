package com.JJIN.domain.mission.entity;

import com.JJIN.domain.mission.entity.enums.MissionImageType;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "mission_image",
	indexes = {
		@Index(name = "idx_mission_image_mission_id", columnList = "mission_id")
	}
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MissionImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id", nullable = false)
	private Mission mission;

	@Column(nullable = false, length = 2048)
	private String imageUrl;

	@Column(nullable = false)
	private int sortOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MissionImageType imageType;

	public static MissionImage of(
		final Mission mission,
		final String imageUrl,
		final int sortOrder,
		final MissionImageType imageType
	) {
		return MissionImage.builder()
			.mission(mission)
			.imageUrl(imageUrl)
			.sortOrder(sortOrder)
			.imageType(imageType)
			.build();
	}
}
