package com.JJIN.domain.place.tourapi.dto;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiIntroItem(
	@JsonProperty("contentid") String contentId,
	@JsonProperty("contenttypeid") String contentTypeId,
	String usetime,
	String restdate,
	String usetimeculture,
	String restdateculture,
	String playtime,
	String eventplace,
	String usetimeleports,
	String restdateleports,
	String opentime,
	String restdateshopping,
	String opentimefood,
	String restdatefood,
	String eventstartdate,
	String eventenddate
) {
	public String openingHours(final TourApiContentType contentType) {
		return switch (contentType) {
			case TOURIST_ATTRACTION -> usetime;
			case CULTURAL_FACILITY -> usetimeculture;
			case FESTIVAL_EVENT -> playtime;
			case LEISURE_SPORTS -> usetimeleports;
			case SHOPPING -> opentime;
			case RESTAURANT -> opentimefood;
			default -> null;
		};
	}

	public String restDay(final TourApiContentType contentType) {
		return switch (contentType) {
			case TOURIST_ATTRACTION -> restdate;
			case CULTURAL_FACILITY -> restdateculture;
			case LEISURE_SPORTS -> restdateleports;
			case SHOPPING -> restdateshopping;
			case RESTAURANT -> restdatefood;
			default -> null;
		};
	}
}
