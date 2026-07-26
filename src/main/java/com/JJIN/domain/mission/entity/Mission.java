package com.JJIN.domain.mission.entity;

import com.JJIN.domain.member.entity.Member;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.mission.entity.enums.MissionSourceType;
import com.JJIN.domain.mission.entity.enums.MissionStatus;
import com.JJIN.global.common.BaseTimeEntity;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mission")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Mission extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String title;

	@Column(nullable = false, length = 150)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MissionDifficulty difficulty;

	// TODO: category, reigion, storeName 추후 enum 화 논의 필요
	@Column(nullable = false, length = 100)
	private String storeName;

	@Column(length = 50)
	private String category;

	@Column(length = 100)
	private String region;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_member_id", nullable = false)
	private Member createdBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MissionSourceType sourceType;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MissionStatus status = MissionStatus.ACTIVE;

	// TODO: 인기 미션 기준이 확정되면 MissionStats 같은 별도 집계 모델로 분리한다.
	@Builder.Default
	@Column(name = "is_recommended", nullable = false)
	private boolean recommended = false;

	public static Mission create(
		final String title,
		final String description,
		final MissionDifficulty difficulty,
		final String storeName,
		final String category,
		final String region,
		final Member createdBy,
		final MissionSourceType sourceType,
		final boolean recommended
	) {
		return Mission.builder()
			.title(title)
			.description(description)
			.difficulty(difficulty)
			.storeName(storeName)
			.category(category)
			.region(region)
			.createdBy(createdBy)
			.sourceType(sourceType)
			.status(MissionStatus.ACTIVE)
			.recommended(recommended)
			.build();
	}
}
