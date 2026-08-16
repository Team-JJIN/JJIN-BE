package com.JJIN.domain.mission.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.JJIN.domain.mission.entity.Mission;

public interface MissionV2Repository extends JpaRepository<Mission, Long> {
}
