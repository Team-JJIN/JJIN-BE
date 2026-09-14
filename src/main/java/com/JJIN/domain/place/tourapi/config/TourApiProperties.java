package com.JJIN.domain.place.tourapi.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "tour-api")
public record TourApiProperties(

	@DefaultValue("https://apis.data.go.kr/B551011") String baseUrl,

	@DefaultValue("") String serviceKey,

	@DefaultValue("ETC") String mobileOs,

	@DefaultValue("JJIN") String mobileApp,

	@DefaultValue("6h") Duration candidateCacheTtl,

	@DefaultValue("24h") Duration detailCacheTtl
) {
}
