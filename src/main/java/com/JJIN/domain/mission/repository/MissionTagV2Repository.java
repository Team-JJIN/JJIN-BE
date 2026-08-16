package com.JJIN.domain.mission.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.JJIN.domain.mission.entity.MissionTag;

public interface MissionTagV2Repository extends JpaRepository<MissionTag, Long> {

	Optional<MissionTag> findByName(String name);
}
