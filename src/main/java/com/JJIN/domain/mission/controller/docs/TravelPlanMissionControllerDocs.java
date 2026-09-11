package com.JJIN.domain.mission.controller.docs;

import org.springframework.http.ResponseEntity;

import com.JJIN.domain.mission.dto.response.TravelPlanMissionListResponse;
import com.JJIN.domain.mission.entity.enums.UserMissionStatus;
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

@Tag(name = "Travel Plan Mission", description = "일정 미션 API")
public interface TravelPlanMissionControllerDocs {

	@Operation(
		summary = "일정 미션 목록 조회",
		description = """
			로그인한 회원이 특정 여행 일정에 담아둔 미션 목록을 최신 추가순으로 조회한다.
			status를 생략하면 전체 목록을, 전달하면 해당 상태의 목록만 반환한다.
			지원 상태는 PROOF_REQUIRED(인증 필요), UPLOAD_PENDING(피드 업로드 대기), COMPLETED(완료)이다.
			상태별 개수는 status 필터 여부와 무관하게 전체 일정 미션을 기준으로 반환한다.
			각 미션의 태그 목록을 tags로 반환하며 태그가 없으면 빈 배열을 반환한다.
			완료 미션은 해당 회원이 그 미션에 작성한 최신 인증글 ID를 missionProofId로 반환한다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "일정 미션 목록 조회 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 200,
					  "message": "일정 미션 목록을 조회했습니다.",
					  "data": {
					    "travelPlanId": 1,
					    "travelPlanName": "여름 마카오 여행",
					    "totalCount": 3,
					    "proofRequiredCount": 1,
					    "uploadPendingCount": 1,
					    "completedCount": 1,
					    "missions": [
					      {
					        "userMissionId": 25,
					        "missionId": 7,
					        "title": "아인슈페너 사먹기",
					        "description": "시그니처 아인슈페너 주문하고 인증샷 남기기",
					        "imageUrl": "https://cdn.example.com/mission/7.jpg",
					        "tags": ["카페투어", "로컬맛집"],
					        "difficulty": "ONE",
					        "status": "PROOF_REQUIRED",
					        "missionProofId": null
					      }
					    ]
					  }
					}
					""")
			)
		),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "본인의 여행 일정을 찾을 수 없음")
	})
	ResponseEntity<SuccessResponse<TravelPlanMissionListResponse>> getTravelPlanMissions(
		CurrentAuth currentAuth,
		@Parameter(description = "여행 일정 ID", example = "1") Long travelPlanId,
		@Parameter(description = "상태 필터: PROOF_REQUIRED, UPLOAD_PENDING, COMPLETED", example = "PROOF_REQUIRED")
		UserMissionStatus status
	);

	@Operation(
		summary = "일정 미션 삭제",
		description = """
			로그인한 회원의 특정 여행 일정에서 선택한 일정 미션을 삭제한다.
			미션 원본과 이미 게시된 인증 피드는 삭제하지 않는다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "일정 미션 삭제 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 200,
					  "message": "일정에서 미션을 삭제했습니다.",
					  "data": null
					}
					""")
			)
		),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "본인의 여행 일정 또는 일정에 담긴 미션을 찾을 수 없음")
	})
	ResponseEntity<SuccessResponse<Void>> deleteTravelPlanMission(
		CurrentAuth currentAuth,
		@Parameter(description = "여행 일정 ID", example = "1") Long travelPlanId,
		@Parameter(description = "일정 미션 ID", example = "25") Long userMissionId
	);
}
