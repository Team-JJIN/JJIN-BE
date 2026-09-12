package com.JJIN.domain.travelplan.controller.docs;

import org.springframework.http.ResponseEntity;

import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.travelplan.dto.request.AddCourseStopRequest;
import com.JJIN.domain.travelplan.dto.request.CreateTravelPlanRequest;
import com.JJIN.domain.travelplan.dto.response.AddCourseStopResponse;
import com.JJIN.domain.travelplan.dto.response.CreateTravelPlanResponse;
import com.JJIN.domain.travelplan.dto.response.TravelCourseDayResponse;
import com.JJIN.domain.travelplan.dto.response.TravelPlanListResponse;
import com.JJIN.global.auth.dto.CurrentAuth;
import com.JJIN.global.response.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Travel Plan", description = "여행 일정 API")
public interface TravelPlanControllerDocs {

	@Operation(
		summary = "여행 일정 목록 조회",
		description = "로그인한 회원의 여행 일정을 최신 생성순으로 조회한다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "여행 일정 목록 조회 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 200,
					  "message": "여행 일정 목록을 조회했습니다.",
					  "data": {
					    "totalCount": 1,
					    "travelPlans": [
					      {
					        "travelPlanId": 1,
					        "name": "마카오 여행",
					        "startDate": "2026-03-08",
					        "endDate": "2026-03-12",
					        "transportMode": "WALKING",
					        "transportModeDisplayName": "도보",
					        "interestCategories": [
					          "RESTAURANT",
					          "TOURIST_ATTRACTION",
					          "CULTURAL_FACILITY"
					        ],
					        "experienceLevel": "LIGHT",
					        "nights": 4,
					        "days": 5
					      }
					    ]
					  }
					}
					""")
			)
		),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음")
	})
	ResponseEntity<SuccessResponse<TravelPlanListResponse>> getTravelPlans(CurrentAuth currentAuth);

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

	@Operation(
		summary = "여행 코스 일차별 방문지 목록 조회",
		description = """
			특정 여행 일정의 코스(방문지 목록)를 일차 기준으로 순서대로 조회한다.
			dayNumber 쿼리 파라미터 생략 시 1일차를 조회한다.
			상단 정보(여행명·일차·해당 일자·전체 일수)와 방문지 목록을 함께 반환하며,
			두 번째 방문지부터는 이전 방문지와의 직선거리(m)를 정수형으로 제공한다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "코스 일차별 방문지 목록 조회 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 200,
					  "message": "여행 코스 일차별 방문지 목록을 조회했습니다.",
					  "data": {
					    "planId": 42,
					    "planName": "여름 마카오 여행",
					    "dayNumber": 1,
					    "totalDays": 3,
					    "date": "2026-03-06",
					    "stopCount": 2,
					    "stops": [
					      {
					        "stopId": 101,
					        "visitOrder": 1,
					        "placeId": 555,
					        "name": "연탄 불고기",
					        "category": "RESTAURANT",
					        "address": "인천광역시 미추홀구 소성로 40",
					        "latitude": 37.4562,
					        "longitude": 126.6543,
					        "openingHoursText": "09:00~19:00",
					        "openStatus": "OPEN",
					        "distanceFromPreviousMeters": null
					      },
					      {
					        "stopId": 102,
					        "visitOrder": 2,
					        "placeId": 556,
					        "name": "연탄 불고기",
					        "category": "RESTAURANT",
					        "address": "인천광역시 미추홀구 소성로 40",
					        "latitude": 37.4583,
					        "longitude": 126.6551,
					        "openingHoursText": "09:00~19:00",
					        "openStatus": "OPEN",
					        "distanceFromPreviousMeters": 230
					      }
					    ]
					  }
					}
					""")
			)
		),
		@ApiResponse(responseCode = "400", description = "여행 기간 범위를 벗어난 일차"),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "403", description = "본인의 여행 일정만 조회 가능"),
		@ApiResponse(responseCode = "404", description = "여행 일정을 찾을 수 없음")
	})
	ResponseEntity<SuccessResponse<TravelCourseDayResponse>> getCourseDay(
		CurrentAuth currentAuth,
		@Parameter(description = "여행 일정 ID", example = "42") Long planId,
		@Parameter(description = "1부터 시작하는 일차 번호. 생략 시 1", example = "1") int dayNumber,
		@Parameter(description = "표시 언어 (KO, EN, JA). 기본값 KO", example = "KO") PlaceLocale locale
	);

	@Operation(
		summary = "여행 코스 방문지 추가",
		description = """
			요청 본문의 dayNumber 일차 코스 마지막에 방문지를 추가한다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "201",
			description = "방문지 추가 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 201,
					  "message": "여행 코스에 방문지를 추가했습니다.",
					  "data": {
					    "stopId": 12,
					    "dayNumber": 1,
					    "visitOrder": 3
					  }
					}
					""")
			)
		),
		@ApiResponse(responseCode = "400", description = "요청 파라미터/일차 범위 오류"),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "403", description = "본인의 여행 일정만 수정 가능"),
		@ApiResponse(responseCode = "404", description = "여행 일정 또는 장소를 찾을 수 없음")
	})
	ResponseEntity<SuccessResponse<AddCourseStopResponse>> addCourseStop(
		CurrentAuth currentAuth,
		@Parameter(description = "여행 일정 ID", example = "42") Long planId,
		AddCourseStopRequest request
	);

	@Operation(
		summary = "여행 코스 방문지 삭제",
		description = """
			코스에서 방문지 하나를 삭제한다.
			같은 일차에서 삭제된 방문지 뒤의 순번(visit_order)은 한 칸씩 앞으로 당겨진다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "방문지 삭제 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 200,
					  "message": "여행 코스에서 방문지를 삭제했습니다.",
					  "data": null
					}
					""")
			)
		),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "403", description = "본인의 여행 일정만 수정 가능"),
		@ApiResponse(responseCode = "404", description = "여행 일정 또는 코스 방문지를 찾을 수 없음")
	})
	ResponseEntity<SuccessResponse<Void>> deleteCourseStop(
		CurrentAuth currentAuth,
		@Parameter(description = "여행 일정 ID", example = "42") Long planId,
		@Parameter(description = "삭제할 코스 방문지 ID", example = "12") Long stopId
	);
}
