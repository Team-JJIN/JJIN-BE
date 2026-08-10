package com.JJIN.domain.mission.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 핫 미션 재집계 스케줄러를 위한 스케줄링 활성화 및 설정 바인딩.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(HotMissionProperties.class)
public class SchedulingConfig {
}
