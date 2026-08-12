package com.JJIN.domain.mission.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.mission.entity.MissionTagMapping;

public interface MissionTagMappingRepository extends JpaRepository<MissionTagMapping, Long> {

	@Query("""
		select mtm
		from MissionTagMapping mtm
		join fetch mtm.tag
		where mtm.mission.id in :missionIds
		order by mtm.mission.id asc, mtm.id asc
		""")
	List<MissionTagMapping> findAllByMissionIdInWithTag(@Param("missionIds") Collection<Long> missionIds);
}
