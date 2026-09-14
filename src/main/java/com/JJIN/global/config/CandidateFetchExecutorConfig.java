package com.JJIN.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 추천 후보 수집(시군구 × 콘텐츠 유형)을 병렬로 조회하기 위한 스레드풀.
 * TourAPI rate limit을 고려해 동시성을 제한한다.
 */
@Configuration
public class CandidateFetchExecutorConfig {

	@Bean(name = "candidateFetchExecutor", destroyMethod = "shutdown")
	public Executor candidateFetchExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(6);
		executor.setMaxPoolSize(6);
		executor.setQueueCapacity(64);
		executor.setThreadNamePrefix("candidate-fetch-");
		executor.initialize();
		return executor.getThreadPoolExecutor();
	}
}
