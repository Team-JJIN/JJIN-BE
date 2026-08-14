package com.JJIN.domain.mission.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.JJIN.domain.mission.dto.internal.MissionSearchCondition;
import com.JJIN.domain.mission.dto.internal.MissionSearchResult;

public interface MissionSearchRepository {

	Page<MissionSearchResult> search(MissionSearchCondition condition, Pageable pageable);
}
