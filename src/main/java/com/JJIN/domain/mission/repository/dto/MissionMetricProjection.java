package com.JJIN.domain.mission.repository.dto;

/**
 * 미션별 지표 집계 결과 프로젝션.
 */
public interface MissionMetricProjection {

	Long getMissionId();

	long getMetric();
}
