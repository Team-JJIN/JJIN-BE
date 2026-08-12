package com.JJIN.domain.mission.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.enums.MissionStatus;

public interface MissionRepository extends JpaRepository<Mission, Long>, MissionSearchRepository {

	@Query("select m from Mission m where m.id in :missionIds")
	List<Mission> findAllByIdIn(@Param("missionIds") Collection<Long> missionIds);

	Optional<Mission> findByIdAndStatus(Long id, MissionStatus status);
}
