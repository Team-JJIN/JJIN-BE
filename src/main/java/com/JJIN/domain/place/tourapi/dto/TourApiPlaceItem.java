package com.JJIN.domain.place.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiPlaceItem(
	@JsonProperty("contentid") String contentId,
	@JsonProperty("contenttypeid") String contentTypeId,
	String title,
	String addr1,
	String addr2,
	String mapx,
	String mapy,
	String firstimage,
	String firstimage2,
	String cpyrhtDivCd,
	String modifiedtime,
	String lDongRegnCd,
	String lDongSignguCd,
	String lclsSystm1,
	String lclsSystm2,
	String overview,
	String eventstartdate,
	String eventenddate
) {
}
