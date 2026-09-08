package com.JJIN.domain.place.tourapi.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 한국관광공사 TourAPI 통합 설정.
 * 인증키/공통 파라미터는 최상위에서 공유하고, 하위 서비스별 튜닝은 중첩 블록으로 분리한다.
 * - place-sync: 지역·상세·축제 등 장소 동기화용 KorService2/EngService2/JpnService2
 * - visitor-stats: 지역별 방문자수 (DataLabService, 로컬도 스코어 원천)
 */
@ConfigurationProperties(prefix = "tour-api")
public record TourApiProperties(

	@DefaultValue("https://apis.data.go.kr/B551011") String baseUrl,

	@DefaultValue("") String serviceKey,

	@DefaultValue("ETC") String mobileOs,

	@DefaultValue("JJIN") String mobileApp,

	@DefaultValue PlaceSync placeSync,

	@DefaultValue VisitorStats visitorStats,

	@DefaultValue AttractionConcentration attractionConcentration
) {

	public record PlaceSync(
		@DefaultValue("6h") Duration candidateCacheTtl,
		@DefaultValue("24h") Duration detailCacheTtl
	) {
	}

	public record VisitorStats(
		@DefaultValue("locgoRegnVisitrDDList") String operation,
		@DefaultValue("24h") Duration cacheTtl,
		@DefaultValue("5000") int pageSize,
		@DefaultValue("10") int maxPages,
		@DefaultValue("30") int lookbackDays
	) {
	}

	/**
	 * KTO 관광지 집중률 API 설정. 시군구 단위 baseline에 per-관광지 modifier를 곱해
	 * 같은 시군구 안에서도 관광지별 로컬도 세밀도를 확보한다.
	 */
	public record AttractionConcentration(
		@DefaultValue("TatsCnctrRateService") String servicePath,
		@DefaultValue("tatsCnctrRatedList") String operation,
		@DefaultValue("24h") Duration cacheTtl,
		@DefaultValue("100") int pageSize,
		@DefaultValue("20") int maxPages,
		@DefaultValue("0.5") double baselineWeight
	) {
	}
}
