package com.JJIN.domain.travelplan.dto.request;

import java.util.List;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;
import com.JJIN.domain.travelplan.dto.internal.CreateTravelPlanCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "여행 일정의 관광타입과 세부 취향")
public record TravelPlanPreferenceRequest(

	@Schema(
		description = "선택 가능한 TourAPI 관광타입",
		example = "RESTAURANT",
		allowableValues = {"TOURIST_ATTRACTION", "CULTURAL_FACILITY", "FESTIVAL_EVENT",
			"LEISURE_SPORTS", "SHOPPING", "RESTAURANT"}
	)
	@NotNull(message = "TourAPI 관광타입은 필수입니다.")
	TourApiContentType contentType,

	@Schema(description = "해당 관광타입에 속한 세부 취향 목록", example = "[\"KOREAN_FOOD\"]")
	@NotEmpty(message = "세부 취향은 최소 1개 이상 선택해야 합니다.")
	List<@NotNull(message = "세부 취향 값이 올바르지 않습니다.") TravelSubcategory> subcategories
) {

	public CreateTravelPlanCommand.Preference toCommand() {
		return new CreateTravelPlanCommand.Preference(contentType, subcategories);
	}
}
