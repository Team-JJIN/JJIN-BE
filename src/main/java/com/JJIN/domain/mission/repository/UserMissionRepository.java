package com.JJIN.domain.mission.repository;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.mission.entity.UserMission;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

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
}
