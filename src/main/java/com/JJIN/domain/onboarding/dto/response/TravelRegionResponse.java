package com.JJIN.domain.onboarding.dto.response;

import com.JJIN.domain.onboarding.entity.TravelRegion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "여행 지역 응답")
public record TravelRegionResponse(

	@Schema(description = "여행 지역 ID", example = "1")
	Long id,

	@Schema(description = "지역 표시명", example = "서울")
	String displayName,

	@Schema(description = "TourAPI KorService2 법정동 시도 코드(lDongRegnCd)", example = "11")
	String lDongRegnCd,

	@Schema(description = "TourAPI KorService2 법정동 시군구 코드(lDongSignguCd)", example = "110", nullable = true)
	String lDongSignguCd
) {

	public static TravelRegionResponse from(final TravelRegion region) {
		return new TravelRegionResponse(
			region.getId(),
			region.getDisplayName(),
			region.getLDongRegnCd(),
			region.getLDongSignguCd()
		);
	}
}
