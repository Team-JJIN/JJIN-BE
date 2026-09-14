package com.JJIN.domain.place.tourapi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TourApiProperties.class)
public class TourApiConfig {

	@Bean
	public RestClient tourApiRestClient(final TourApiProperties properties) {
		return RestClient.builder()
			.baseUrl(properties.baseUrl())
			.build();
	}
}
