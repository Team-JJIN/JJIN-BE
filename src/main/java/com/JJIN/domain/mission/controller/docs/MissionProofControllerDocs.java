package com.JJIN.domain.mission.controller.docs;

import org.springframework.http.ResponseEntity;

import com.JJIN.domain.mission.dto.response.MissionProofFeedResponse;
import com.JJIN.domain.mission.dto.response.MissionProofLikeToggleResponse;
import com.JJIN.domain.mission.entity.enums.MissionProofFeedTab;
import com.JJIN.global.auth.annotation.CurrentMember;
import com.JJIN.global.auth.dto.CurrentAuth;
import com.JJIN.global.response.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Mission Proof", description = "미션 인증 피드 API")
public interface MissionProofControllerDocs {

	@Operation(
		summary = "미션 인증 피드 조회",
		description = """
			상단 탭 기준으로 미션 인증글 피드를 페이지 단위로 조회한다.

			- tab: LATEST(최신) | POPULAR(인기) | WEEKLY_HOT(이번 주 핫) | COMPLETED(완료 미션), 기본 LATEST
			- LATEST: 작성 시각 최신순
			- POPULAR: 좋아요 많은 순
			- WEEKLY_HOT: '요즘 핫한 미션'(HotMission 집계) 스냅샷에 선정된 미션의 인증글을 스냅샷 랭킹순으로 우선 노출 (스냅샷이 없으면 빈 목록)
			- COMPLETED: 현재 로그인 사용자가 완료한 미션의 인증글만 최신순
			- size는 최대 50으로 제한된다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponse(
		responseCode = "200",
		description = "미션 인증 피드 조회 성공",
		content = @Content(
			mediaType = "application/json",
			examples = @ExampleObject(value = """
				{
				  "status": 200,
				  "message": "미션 인증 피드를 조회했습니다.",
				  "data": {
				    "tab": "LATEST",
				    "page": 0,
				    "size": 10,
				    "hasNext": true,
				    "items": [
				      {
				        "proofId": 1,
				        "author": {
				          "memberId": 1,
				          "nickname": "지민",
				          "profileImageUrl": null
				        },
				        "imageUrl": "https://cdn.example.com/proof/1.jpg",
				        "content": "성수 매머드 아인슈페너 크림 진짜 미쳤음.",
				        "likeCount": 42,
				        "commentCount": 0,
				        "likedByMe": true,
				        "createdAt": "2026-08-11T02:09:15",
				        "mission": {
				          "missionId": 10,
				          "title": "매머드 커피 아인슈페너 사먹기",
				          "difficulty": "ONE",
				          "weeklyCompletedCount": 128
				        }
				      }
				    ]
				  }
				}
				""")
		)
	)
	ResponseEntity<SuccessResponse<MissionProofFeedResponse>> getMissionProofFeed(
		@CurrentMember CurrentAuth currentAuth,
		@Parameter(description = "피드 탭", example = "LATEST") MissionProofFeedTab tab,
		@Parameter(description = "페이지 번호(0부터)", example = "0") int page,
		@Parameter(description = "페이지 크기(최대 50)", example = "10") int size
	);

	@Operation(
		summary = "미션 인증 좋아요 토글",
		description = """
			인증글 좋아요를 토글한다.

			- 좋아요가 없으면 생성하고 likeCount +1
			- 좋아요가 있으면 삭제하고 likeCount -1 (0 미만으로 내려가지 않음)
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponse(
		responseCode = "200",
		description = "미션 인증 좋아요 상태 변경 성공",
		content = @Content(
			mediaType = "application/json",
			examples = @ExampleObject(value = """
				{
				  "status": 200,
				  "message": "미션 인증 좋아요 상태를 변경했습니다.",
				  "data": {
				    "proofId": 1,
				    "liked": true,
				    "likeCount": 43
				  }
				}
				""")
		)
	)
	ResponseEntity<SuccessResponse<MissionProofLikeToggleResponse>> toggleMissionProofLike(
		@CurrentMember CurrentAuth currentAuth,
		@Parameter(description = "인증글 ID", example = "1") Long proofId
	);
}
