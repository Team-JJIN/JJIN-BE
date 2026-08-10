package com.JJIN.domain.mission.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * '요즘 핫한 미션' 집계 설정.
 *
 * <ul>
 *   <li>windowDays: 집계 및 고정 기간(일). 이 기간 동안 핫 미션은 바뀌지 않는다.</li>
 *   <li>topN: 노출할 상위 미션 개수.</li>
 * </ul>
 * 순위는 기간 내 미션 추가(담기) 횟수로 매긴다.
 */
@ConfigurationProperties(prefix = "hot-mission")
public record HotMissionProperties(

	@DefaultValue("4") int windowDays,

	@DefaultValue("5") int topN
) {
}
