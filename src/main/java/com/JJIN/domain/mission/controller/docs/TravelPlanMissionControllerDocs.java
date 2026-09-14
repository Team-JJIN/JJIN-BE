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
			인증 화면에서 사진을 S3에 업로드한 뒤 '인증 완료'를 누르면 호출한다.
			Presigned URL 응답의 fileName을 proofImageKey로 전달한다.
			사진 key와 인증 시각을 저장하고 PROOF_REQUIRED에서 UPLOAD_PENDING으로 변경한다.
			피드 게시글을 생성하거나 COMPLETED로 변경하지 않는다. 피드 미게시 선택 시에도 UPLOAD_PENDING을 유지한다.
			사진 key 형식만 검증하며 실제 S3 업로드 여부나 사진 내용의 진위는 검사하지 않는다.
			동일 key로 재요청하면 기존 결과를 반환한다. 다른 key로 재인증하거나 완료된 미션을 인증하면 409를 반환한다.
			응답의 proofImageUrl은 1시간 유효한 사진 표시용 Presigned GET URL이며 DB에 저장하지 않는다.
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
				    "proofImageKey": "mission-proof/550e8400-e29b-41d4-a716-446655440000_proof.jpg",
				    "proofImageUrl": "https://example-bucket.s3.ap-northeast-2.amazonaws.com/mission-proof/...?X-Amz-Signature=...",
				    "authenticatedAt": "2026-09-14T23:40:00"
				  }
				}
				"""))
		),
		@ApiResponse(responseCode = "400", description = "사진 key 누락, 길이 초과 또는 형식 오류"),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "본인의 일정 또는 일정 미션을 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "이미 인증된 일정 미션"),
		@ApiResponse(responseCode = "500", description = "사진 표시용 URL 생성 실패")
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
			인증 화면과 피드 작성 화면 진입 시 호출한다. 피드 제목과 내용은 반환하지 않는다.
			사진 인증 전에도 200으로 미션 제목과 상태를 반환하며 사진 key, URL과 인증 시각은 null이다.
			인증 후 proofImageUrl은 조회할 때마다 발급하는 1시간 유효 Presigned GET URL이다.
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
				    "proofImageKey": "mission-proof/550e8400-e29b-41d4-a716-446655440000_proof.jpg",
				    "proofImageUrl": "https://example-bucket.s3.ap-northeast-2.amazonaws.com/mission-proof/...?X-Amz-Signature=...",
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
				    "proofImageKey": null,
				    "proofImageUrl": null,
				    "authenticatedAt": null
				  }
				}
				""")
			})
		),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "본인의 일정 또는 일정 미션을 찾을 수 없음"),
		@ApiResponse(responseCode = "500", description = "사진 표시용 URL 생성 실패")
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
