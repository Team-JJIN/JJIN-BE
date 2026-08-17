package com.JJIN.domain.mission.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.mission.entity.UserMission;
import com.JJIN.domain.mission.repository.dto.MissionMetricProjection;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

	Optional<UserMission> findByTravelPlanIdAndMissionId(Long travelPlanId, Long missionId);

	List<UserMission> findAllByMemberIdAndMissionId(Long memberId, Long missionId);

	@Query("select distinct um.mission.id from UserMission um where um.member.id = :memberId")
	List<Long> findDistinctMissionIdsByMemberId(@Param("memberId") Long memberId);

	@Query("""
		select um.mission.id
		from UserMission um
		where um.member.id = :memberId
		and um.mission.id in :missionIds
		""")
	Set<Long> findMissionIdsByMemberIdAndMissionIdIn(
		@Param("memberId") Long memberId,
		@Param("missionIds") Collection<Long> missionIds
	);

	boolean existsByMemberIdAndMissionId(Long memberId, Long missionId);

	/**
	 * 기간 [start, end) 동안 미션별 추가(담기) 횟수를 집계한다.
	 */
	@Query("""
		select um.mission.id as missionId, count(um) as metric
		from UserMission um
		where um.addedAt >= :start and um.addedAt < :end
		group by um.mission.id
		""")
	List<MissionMetricProjection> countAddedByMissionInWindow(
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end
	);

	/**
	 * 이번 주 [weekStart, weekEnd) 동안 미션별 완료 수(status=COMPLETED)를 집계한다.
	 */
	@Query("""
		select um.mission.id as missionId, count(um) as metric
		from UserMission um
		where um.mission.id in :missionIds
		  and um.status = com.JJIN.domain.mission.entity.enums.UserMissionStatus.COMPLETED
		  and um.completedAt >= :weekStart and um.completedAt < :weekEnd
		group by um.mission.id
		""")
	List<MissionMetricProjection> countWeeklyCompletedByMissionIds(
		@Param("missionIds") Collection<Long> missionIds,
		@Param("weekStart") LocalDateTime weekStart,
		@Param("weekEnd") LocalDateTime weekEnd
	);
}
