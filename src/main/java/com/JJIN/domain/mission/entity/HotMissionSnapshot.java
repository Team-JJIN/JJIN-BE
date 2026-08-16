package com.JJIN.domain.mission.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * '요즘 핫한 미션' 집계 스냅샷(배치).
 *
 * 스케줄러가 windowDays 주기로 새 스냅샷을 생성하며, 노출 시점에는 항상 가장 최근 스냅샷을 조회한다.
 * expiresAt 이전에는 재집계하지 않으므로 해당 기간 동안 핫 미션 목록이 고정된다.
 */
@Entity
@Table(
	name = "hot_mission_snapshot",
	indexes = {
		@Index(name = "idx_hot_mission_snapshot_computed_at", columnList = "computed_at")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HotMissionSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 집계 대상 기간 시작(포함) */
	@Column(name = "window_start", nullable = false)
	private LocalDateTime windowStart;

	/** 집계 대상 기간 종료(제외) */
	@Column(name = "window_end", nullable = false)
	private LocalDateTime windowEnd;

	/** 스냅샷 생성 시각 */
	@Column(name = "computed_at", nullable = false)
	private LocalDateTime computedAt;

	/** 이 시각 이후 첫 스케줄에서 재집계된다. 이전에는 목록이 고정된다. */
	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HotMissionItem> items = new ArrayList<>();

	@Builder
	private HotMissionSnapshot(
		final LocalDateTime windowStart,
		final LocalDateTime windowEnd,
		final LocalDateTime computedAt,
		final LocalDateTime expiresAt
	) {
		this.windowStart = windowStart;
		this.windowEnd = windowEnd;
		this.computedAt = computedAt;
		this.expiresAt = expiresAt;
	}

	public static HotMissionSnapshot create(
		final LocalDateTime windowStart,
		final LocalDateTime windowEnd,
		final LocalDateTime computedAt,
		final LocalDateTime expiresAt
	) {
		return HotMissionSnapshot.builder()
			.windowStart(windowStart)
			.windowEnd(windowEnd)
			.computedAt(computedAt)
			.expiresAt(expiresAt)
			.build();
	}

	/**
	 * 미션 항목을 추가하며 양방향 연관관계를 설정한다.
	 */
	public HotMissionItem addItem(
		final Mission mission,
		final long addedCount
	) {
		HotMissionItem item = HotMissionItem.of(this, mission, addedCount);
		this.items.add(item);
		return item;
	}

	public boolean isExpiredAt(final LocalDateTime now) {
		return !now.isBefore(expiresAt);
	}
}
