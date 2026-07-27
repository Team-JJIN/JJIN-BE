package com.JJIN.domain.onboarding.dto.request;

import java.util.List;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "TourAPI 관광타입 1개와 그 하위 온보딩 세부 취향 선택")
public record ContentTypePreferenceRequest(

	@Schema(
		description = "TourAPI 관광타입. 온보딩 취향 선택에서는 TRAVEL_COURSE, LODGING을 선택할 수 없다.",
		example = "RESTAURANT",
		allowableValues = {"TOURIST_ATTRACTION", "CULTURAL_FACILITY", "FESTIVAL_EVENT",
			"LEISURE_SPORTS", "SHOPPING", "RESTAURANT"}
	)
	@NotNull(message = "TourAPI 관광타입은 필수입니다.")
	TourApiContentType contentType,

	@Schema(
		description = "해당 TourAPI 관광타입에 속한 세부 취향 목록 (최소 1개, 중복 불가)",
		example = "[\"KOREAN_FOOD\", \"CAFE_TEAHOUSE\"]"
	)
	@NotEmpty(message = "세부 취향은 최소 1개 이상 선택해야 합니다.")
	List<@NotNull(message = "세부 취향 값이 올바르지 않습니다.") TravelSubcategory> subcategories
) {
}
