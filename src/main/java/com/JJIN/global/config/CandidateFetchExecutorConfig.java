package com.JJIN.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 추천 후보 수집(시군구 × 콘텐츠 유형)·운영시간 enrich를 병렬로 조회하기 위한 스레드풀.
 * TourAPI rate limit을 고려해 동시성을 제한한다.
 * 큐가 가득 차면 제출 스레드가 직접 태스크를 실행(CallerRunsPolicy)해 백프레셔로 처리한다.
 * 태스크 거부(RejectedExecutionException)로 요청이 실패하지 않도록 하기 위함이다.
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
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor.getThreadPoolExecutor();
	}
}
