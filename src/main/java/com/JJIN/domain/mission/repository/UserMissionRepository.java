package com.JJIN.domain.mission.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.mission.entity.UserMission;
import com.JJIN.domain.mission.repository.dto.MissionMetricProjection;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

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
}
