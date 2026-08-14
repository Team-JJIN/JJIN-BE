package com.JJIN.domain.mission.entity;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 핫 미션 스냅샷에 포함된 미션 항목.
 * 집계 당시의 추가수를 그대로 보관해 노출 시점에 재계산이 필요 없도록 한다.
 * 노출 순서는 추가수 내림차순으로 정렬해 얻는다.
 */
@Entity
@Table(
	name = "hot_mission_item",
	indexes = {
		@Index(name = "idx_hot_mission_item_snapshot_id", columnList = "snapshot_id")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HotMissionItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "snapshot_id", nullable = false)
	private HotMissionSnapshot snapshot;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id", nullable = false)
	private Mission mission;

	/** 집계 기간 내 미션 추가(담기) 횟수 */
	@Column(name = "added_count", nullable = false)
	private long addedCount;

	@Builder
	private HotMissionItem(
		final HotMissionSnapshot snapshot,
		final Mission mission,
		final long addedCount
	) {
		this.snapshot = snapshot;
		this.mission = mission;
		this.addedCount = addedCount;
	}

	public static HotMissionItem of(
		final HotMissionSnapshot snapshot,
		final Mission mission,
		final long addedCount
	) {
		return HotMissionItem.builder()
			.snapshot(snapshot)
			.mission(mission)
			.addedCount(addedCount)
			.build();
	}
}
