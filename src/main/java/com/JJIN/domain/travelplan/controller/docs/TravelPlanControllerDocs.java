package com.JJIN.domain.travelplan.controller.docs;

import org.springframework.http.ResponseEntity;

import com.JJIN.domain.travelplan.dto.request.CreateTravelPlanRequest;
import com.JJIN.domain.travelplan.dto.response.CreateTravelPlanResponse;
import com.JJIN.global.auth.dto.CurrentAuth;
import com.JJIN.global.response.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Travel Plan", description = "여행 일정 API")
public interface TravelPlanControllerDocs {

	@Operation(
		summary = "여행 일정 생성",
		description = """
			기존 온보딩 화면을 일정 생성 화면으로 재사용하여 여행 기본 정보와 취향을 저장한다.
			이 API는 빈 여행 일정만 생성하며 장소 추천이나 장소 추가를 실행하지 않는다.
			지역 미정 여부와 지역 ID, 여행 날짜, 활동 시간, 관광타입과 세부 취향 조합을 검증한다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "201",
			description = "여행 일정 생성 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 201,
					  "message": "여행 일정을 생성했습니다.",
					  "data": {
					    "travelPlanId": 1
					  }
					}
					""")
			)
		),
		@ApiResponse(responseCode = "400", description = "여행 일정 입력값 또는 조합이 올바르지 않음"),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
	})
	ResponseEntity<SuccessResponse<CreateTravelPlanResponse>> createTravelPlan(
		CurrentAuth currentAuth,
		CreateTravelPlanRequest request
	);
}
