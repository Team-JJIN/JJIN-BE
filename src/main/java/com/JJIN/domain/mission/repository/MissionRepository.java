package com.JJIN.domain.mission.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.enums.MissionStatus;

public interface MissionRepository extends JpaRepository<Mission, Long> {

	List<Mission> findAllByIdInAndStatus(Collection<Long> ids, MissionStatus status);
}
