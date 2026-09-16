package com.JJIN.domain.mission.controller.docs;

import org.springframework.http.ResponseEntity;

import com.JJIN.domain.mission.dto.request.TravelPlanMissionAuthenticateRequest;
import com.JJIN.domain.mission.dto.response.TravelPlanMissionAuthenticationResponse;
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
		summary = "일정 미션 사진 인증",
		description = """
			인증 화면의 미션 제목은 이전 일정 미션 목록 조회 응답의 title을 전달받아 표시한다.
			촬영한 사진은 클라이언트에서 미리보기로 표시하며, 이 단계에서 인증 사진 조회 API를 호출할 필요는 없다.
			사진 촬영 후 POST /api/missions/proofs/presigned-url로 업로드 URL을 발급받고 해당 URL로 S3에 직접 PUT 업로드한다.
			인증 화면에서 사진을 S3에 업로드한 뒤 '인증 완료'를 누르면 호출한다.
			PUT Presigned URL에서 '?' 이후 서명 쿼리를 제거한 공개 raw URL을 proofImageUrl로 전달한다.
			미션 제목과 피드 제목·내용은 요청에 포함하지 않는다.
			사진 raw URL과 인증 시각을 저장하고 PROOF_REQUIRED에서 UPLOAD_PENDING으로 변경한다.
			피드 게시글을 생성하거나 COMPLETED로 변경하지 않는다. 피드 미게시 선택 시에도 UPLOAD_PENDING을 유지한다.
			쿼리와 fragment가 없는 HTTPS URL 형식만 검증하며 실제 업로드 여부, 사진 내용의 진위 및 객체 소유권은 검사하지 않는다.
			동일 URL로 재요청하면 최초 인증 시각과 URL을 유지한다. 다른 URL로 재인증하거나 완료된 미션을 인증하면 409를 반환한다.
			응답의 proofImageUrl은 만료되지 않는 공개 S3 객체 URL이다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200", description = "사진 인증 저장 성공",
			content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
				{
				  "status": 200,
				  "message": "일정 미션을 인증했습니다.",
				  "data": {
				    "travelPlanId": 1,
				    "userMissionId": 25,
				    "missionId": 7,
				    "missionTitle": "아인슈페너 사먹기",
				    "status": "UPLOAD_PENDING",
				    "proofImageUrl": "https://example-bucket.s3.ap-northeast-2.amazonaws.com/mission-proof/550e8400-e29b-41d4-a716-446655440000_proof.jpg",
				    "authenticatedAt": "2026-09-14T23:40:00"
				  }
				}
				"""))
		),
		@ApiResponse(responseCode = "400", description = "사진 URL 누락, 길이 초과 또는 형식 오류"),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "본인의 일정 또는 일정 미션을 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "이미 인증된 일정 미션")
	})
	ResponseEntity<SuccessResponse<TravelPlanMissionAuthenticationResponse>> authenticateTravelPlanMission(
		CurrentAuth currentAuth,
		@Parameter(description = "여행 일정 ID", example = "1") Long travelPlanId,
		@Parameter(description = "일정 미션 ID", example = "25") Long userMissionId,
		TravelPlanMissionAuthenticateRequest request
	);

	@Operation(
		summary = "일정 미션 인증 사진 조회",
		description = """
			본인 일정 미션의 제목, 상태와 인증 사진 정보를 조회한다.
			피드 작성 화면 진입 또는 저장된 인증 사진을 다시 표시할 때 호출한다. 피드 제목과 내용은 반환하지 않는다.
			최초 인증 화면 진입 시에는 이전 일정 미션 목록 조회 응답의 title을 사용하므로 이 API 호출은 필요하지 않다.
			촬영 직후 사진은 클라이언트 미리보기로 표시한다. 인증 완료 API 호출 전에는 사진 URL이 DB에 저장되지 않는다.
			사진 인증 전에도 200으로 미션 제목과 상태를 반환하며 사진 URL과 인증 시각은 null이다.
			인증 후 proofImageUrl은 DB에 저장된 공개 raw URL을 그대로 반환한다.
			S3 버킷 또는 해당 객체가 공개 읽기를 허용해야 클라이언트에서 이 URL로 사진을 표시할 수 있다.
			조회로 미션 상태를 변경하지 않는다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200", description = "인증 사진 조회 성공",
			content = @Content(mediaType = "application/json", examples = {
				@ExampleObject(name = "인증 후", value = """
				{
				  "status": 200,
				  "message": "일정 미션 인증 사진을 조회했습니다.",
				  "data": {
				    "travelPlanId": 1,
				    "userMissionId": 25,
				    "missionId": 7,
				    "missionTitle": "아인슈페너 사먹기",
				    "status": "UPLOAD_PENDING",
				    "proofImageUrl": "https://example-bucket.s3.ap-northeast-2.amazonaws.com/mission-proof/550e8400-e29b-41d4-a716-446655440000_proof.jpg",
				    "authenticatedAt": "2026-09-14T23:40:00"
				  }
				}
				"""),
				@ExampleObject(name = "인증 전", value = """
				{
				  "status": 200,
				  "message": "일정 미션 인증 사진을 조회했습니다.",
				  "data": {
				    "travelPlanId": 1,
				    "userMissionId": 25,
				    "missionId": 7,
				    "missionTitle": "아인슈페너 사먹기",
				    "status": "PROOF_REQUIRED",
				    "proofImageUrl": null,
				    "authenticatedAt": null
				  }
				}
				""")
			})
		),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "본인의 일정 또는 일정 미션을 찾을 수 없음")
	})
	ResponseEntity<SuccessResponse<TravelPlanMissionAuthenticationResponse>> getTravelPlanMissionAuthentication(
		CurrentAuth currentAuth,
		@Parameter(description = "여행 일정 ID", example = "1") Long travelPlanId,
		@Parameter(description = "일정 미션 ID", example = "25") Long userMissionId
	);

	@Operation(
		summary = "일정 미션 목록 조회",
		description = """
			로그인한 회원이 특정 여행 일정에 담아둔 미션 목록을 최신 추가순으로 조회한다.
			status를 생략하면 전체 목록을, 전달하면 해당 상태의 목록만 반환한다.
			지원 상태는 PROOF_REQUIRED(인증 필요), UPLOAD_PENDING(피드 업로드 대기), COMPLETED(완료)이다.
			상태별 개수는 status 필터 여부와 무관하게 전체 일정 미션을 기준으로 반환한다.
			각 미션의 태그 목록을 tags로 반환하며 태그가 없으면 빈 배열을 반환한다.
			'인증하기' 선택 시 userMissionId와 title을 인증 화면으로 전달하여 제목을 표시한다.
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
