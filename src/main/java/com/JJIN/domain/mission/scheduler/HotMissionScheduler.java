package com.JJIN.domain.mission.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.JJIN.domain.mission.service.HotMissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 핫 미션 재집계 트리거.
 *
 * 주기적으로 만료 여부만 확인하고, 만료된 경우에만 실제 재집계가 수행된다(HotMissionService.refreshIfDue).
 * 따라서 확인 주기(cron)를 자주 돌려도 스냅샷은 windowDays 동안 고정된 채 유지된다.
 * 재집계 시점을 스냅샷의 expiresAt으로 관리하므로 재시작/다운타임에도 고정 기간이 흐트러지지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotMissionScheduler {

	private final HotMissionService hotMissionService;

	/**
	 * 애플리케이션 기동 직후, 아직 스냅샷이 없으면 최초 집계를 수행한다.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void seedOnStartup() {
		safeRefresh();
	}

	/**
	 * 매시 정각에 만료 여부를 확인한다. cron은 설정으로 조정 가능하다.
	 */
	@Scheduled(cron = "${hot-mission.refresh-cron:0 0 * * * *}")
	public void refresh() {
		safeRefresh();
	}

	private void safeRefresh() {
		try {
			hotMissionService.refreshIfDue();
		} catch (Exception e) {
			// 스케줄러 스레드가 죽지 않도록 예외를 삼키고 다음 주기에 재시도한다.
			log.error("핫 미션 재집계 중 오류가 발생했습니다.", e);
		}
	}
}
