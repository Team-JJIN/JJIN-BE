package com.JJIN.domain.mission.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.mission.config.HotMissionProperties;
import com.JJIN.domain.mission.dto.response.HotMissionListResponse;
import com.JJIN.domain.mission.entity.HotMissionSnapshot;
import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.enums.MissionStatus;
import com.JJIN.domain.mission.repository.HotMissionSnapshotRepository;
import com.JJIN.domain.mission.repository.MissionRepository;
import com.JJIN.domain.mission.repository.UserMissionRepository;
import com.JJIN.domain.mission.repository.dto.MissionMetricProjection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * '요즘 핫한 미션' 집계/조회 서비스.
 *
 * 최근 windowDays 기간 동안 미션이 추가(담기)된 횟수를 기준으로 상위 N개를 스냅샷으로 고정한다.
 * 스냅샷은 만료(expiresAt) 전까지 재집계되지 않으므로 해당 기간 동안 핫 미션 목록이 변하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotMissionService {

	private final HotMissionSnapshotRepository hotMissionSnapshotRepository;
	private final UserMissionRepository userMissionRepository;
	private final MissionRepository missionRepository;
	private final HotMissionProperties properties;
	private final Clock clock;

	/**
	 * 현재 고정된 핫 미션 목록을 조회한다. 아직 집계된 스냅샷이 없으면 빈 목록을 반환한다.
	 */
	@Transactional(readOnly = true)
	public HotMissionListResponse getCurrentHotMissions() {
		return hotMissionSnapshotRepository.findTopByOrderByComputedAtDesc()
			.map(snapshot -> HotMissionListResponse.of(
				snapshot,
				hotMissionSnapshotRepository.findItemsWithMission(snapshot)
			))
			.orElseGet(HotMissionListResponse::empty);
	}

	/**
	 * 현재 스냅샷이 없거나 만료되었으면 새로 집계한다.
	 * 만료 전이면 아무 것도 하지 않아 노출 목록이 고정된다.
	 * 스케줄러와 애플리케이션 기동 시점에서 동시에 호출될 수 있어 동기화한다.
	 */
	@Transactional
	public synchronized void refreshIfDue() {
		LocalDateTime now = LocalDateTime.now(clock);

		boolean due = hotMissionSnapshotRepository.findTopByOrderByComputedAtDesc()
			.map(snapshot -> snapshot.isExpiredAt(now))
			.orElse(true);

		if (!due) {
			log.debug("핫 미션 스냅샷이 아직 유효하여 재집계를 건너뜁니다.");
			return;
		}
		recompute(now);
	}

	private void recompute(final LocalDateTime now) {
		int windowDays = properties.windowDays();
		LocalDateTime windowStart = now.minusDays(windowDays);
		LocalDateTime windowEnd = now;
		LocalDateTime expiresAt = now.plusDays(windowDays);

		List<MissionMetricProjection> addedCounts =
			userMissionRepository.countAddedByMissionInWindow(windowStart, windowEnd);

		HotMissionSnapshot snapshot = HotMissionSnapshot.create(windowStart, windowEnd, now, expiresAt);

		// 활성(ACTIVE) 미션으로 한정해 추가수 상위 N개를 항목으로 담는다.
		int itemCount = 0;
		if (!addedCounts.isEmpty()) {
			Map<Long, Mission> activeMissions = missionRepository.findAllByIdInAndStatus(
					addedCounts.stream().map(MissionMetricProjection::getMissionId).toList(),
					MissionStatus.ACTIVE
				).stream()
				.collect(Collectors.toMap(Mission::getId, mission -> mission));

			List<MissionMetricProjection> ranked = addedCounts.stream()
				.filter(row -> activeMissions.containsKey(row.getMissionId()))
				.filter(row -> row.getMetric() > 0)
				// 추가수 내림차순, 동점 시 미션 ID 오름차순으로 결정적 정렬
				.sorted(Comparator.comparingLong(MissionMetricProjection::getMetric).reversed()
					.thenComparing(MissionMetricProjection::getMissionId))
				.limit(properties.topN())
				.toList();

			for (MissionMetricProjection row : ranked) {
				snapshot.addItem(activeMissions.get(row.getMissionId()), row.getMetric());
			}
			itemCount = ranked.size();
		}

		HotMissionSnapshot saved = hotMissionSnapshotRepository.save(snapshot);
		// '요즘 핫한'은 항상 최신 스냅샷만 사용하므로 과거 스냅샷은 누적하지 않고 정리한다.
		pruneOldSnapshots(saved.getId());

		log.info("핫 미션 집계 완료: window=[{} ~ {}), items={}, expiresAt={}",
			windowStart, windowEnd, itemCount, expiresAt);
	}

	/**
	 * 최신 스냅샷 하나만 남기고 과거 스냅샷을 삭제한다.
	 * cascade/orphanRemoval로 하위 항목(HotMissionItem)도 함께 제거된다.
	 */
	private void pruneOldSnapshots(final Long latestSnapshotId) {
		List<HotMissionSnapshot> old = hotMissionSnapshotRepository.findByIdNot(latestSnapshotId);
		if (!old.isEmpty()) {
			hotMissionSnapshotRepository.deleteAll(old);
		}
	}
}
