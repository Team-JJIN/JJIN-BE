package com.JJIN.domain.mission.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.mission.entity.HotMissionItem;
import com.JJIN.domain.mission.entity.HotMissionSnapshot;

public interface HotMissionSnapshotRepository extends JpaRepository<HotMissionSnapshot, Long> {

	/**
	 * 가장 최근에 집계된 스냅샷. 노출/만료 판단의 기준이 된다.
	 */
	Optional<HotMissionSnapshot> findTopByOrderByComputedAtDesc();

	/**
	 * 최신 스냅샷을 제외한 나머지(과거) 스냅샷. 재집계 후 정리 용도.
	 */
	List<HotMissionSnapshot> findByIdNot(Long id);

	/**
	 * 스냅샷에 속한 항목을 미션과 함께 추가수 내림차순(동점 시 미션 ID 오름차순)으로 조회한다.
	 */
	@Query("""
		select i from HotMissionItem i
		join fetch i.mission m
		where i.snapshot = :snapshot
		order by i.addedCount desc, m.id asc
		""")
	List<HotMissionItem> findItemsWithMission(@Param("snapshot") HotMissionSnapshot snapshot);
}
