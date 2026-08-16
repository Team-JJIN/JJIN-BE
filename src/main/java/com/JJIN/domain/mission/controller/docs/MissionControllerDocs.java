package com.JJIN.domain.mission.controller.docs;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.JJIN.domain.mission.dto.response.HotMissionListResponse;
import com.JJIN.domain.mission.dto.response.MissionDetailResponse;
import com.JJIN.domain.mission.dto.response.MissionSearchFeedResponse;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
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

@Tag(name = "Mission", description = "미션 API")
public interface MissionControllerDocs {

	@Operation(
		summary = "미션 검색 피드 목록 조회",
		description = """
			미션명, 태그명을 keyword로 검색하고 카테고리/난이도 다중 필터를 적용한다.
			특정 카테고리 또는 난이도를 눌렀을 때는 categories/difficulties에 해당 코드를 전달한다.
			예: /api/missions?categories=RESTAURANT&difficulties=ONE&page=0&size=20
			sort는 popular/latest를 지원하며 생략 시 popular가 기본값이다.
			카테고리는 기존 TourApiContentType enum을 재사용한다.
			미션 추가/추가 취소 동작은 제공하지 않고, isAdded는 UserMission 기반 읽기 전용 상태만 내려준다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "미션 검색 피드 조회 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 200,
					  "message": "미션 검색 피드 조회에 성공했습니다.",
					  "data": {
					    "missions": [
					      {
					        "missionId": 1,
					        "title": "전통시장 한 끼 먹기",
					        "thumbnailImageUrl": "https://cdn.example.com/mission/1.jpg",
					        "tags": ["음식", "전통시장"],
					        "category": "RESTAURANT",
					        "difficulty": "TWO",
					        "popularity": 12,
					        "createdAt": "2026-08-12T10:30:00",
					        "isAdded": true
					      }
					    ],
					    "totalMissionCount": 53,
					    "page": 0,
					    "size": 20,
					    "hasNext": true
					  }
					}
					""")
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 sort 또는 페이지 요청 값",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 400,
					  "message": "지원하지 않는 미션 정렬값입니다."
					}
					""")
			)
		)
	})
	ResponseEntity<SuccessResponse<MissionSearchFeedResponse>> searchMissions(
		CurrentAuth currentAuth,
		@Parameter(description = "미션명, 태그 검색어", example = "전통시장") String keyword,
		@Parameter(description = "카테고리 코드 목록. 예: categories=RESTAURANT&categories=SHOPPING")
		List<TourApiContentType> categories,
		@Parameter(description = "난이도 코드 목록. 예: difficulties=ONE&difficulties=TWO")
		List<MissionDifficulty> difficulties,
		@Parameter(description = "정렬값: popular/latest", example = "popular") String sort,
		@Parameter(description = "페이지 번호. 0부터 시작", example = "0") int page,
		@Parameter(description = "페이지 크기. 1~50", example = "20") int size
	);

	@Operation(
		summary = "미션 상세 조회",
		description = """
			미션 카드 썸네일/제목 탭 시 상세 팝업에 표시할 정보를 조회한다.
			미션 추가/추가 취소 동작은 제공하지 않고, isAdded는 UserMission 기반 읽기 전용 상태만 내려준다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "미션 상세 조회 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 200,
					  "message": "미션 상세 조회에 성공했습니다.",
					  "data": {
					    "missionId": 1,
					    "title": "전통시장 한 끼 먹기",
					    "representativeImageUrl": "https://cdn.example.com/mission/1.jpg",
					    "description": "지역 전통시장에서 한 끼를 먹고 인증 사진을 남겨보세요.",
					    "tags": ["음식", "전통시장"],
					    "category": "RESTAURANT",
					    "difficulty": "TWO",
					    "isAdded": true
					  }
					}
					""")
			)
		),
		@ApiResponse(
			responseCode = "404",
			description = "미션을 찾을 수 없음",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 404,
					  "message": "미션을 찾을 수 없습니다."
					}
					""")
			)
		)
	})
	ResponseEntity<SuccessResponse<MissionDetailResponse>> getMission(
		CurrentAuth currentAuth,
		Long missionId
	);

	@Operation(
		summary = "요즘 핫한 미션 조회",
		description = """
			최근 N일(기본 4일) 동안 미션이 추가(담기)된 횟수를 집계해 선정한 '요즘 핫한 미션' 상위 목록을 반환한다.

			- 집계 지표: 기간 내 미션 추가 수 (동점 시 미션 ID 오름차순)
			- 결과는 스냅샷으로 고정되어 nextRefreshAt 이전에는 목록이 바뀌지 않는다.
			- 아직 집계된 스냅샷이 없으면 missions는 빈 배열로 반환된다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponse(
		responseCode = "200",
		description = "요즘 핫한 미션 목록 조회 성공",
		content = @Content(
			mediaType = "application/json",
			examples = @ExampleObject(value = """
				{
				  "status": 200,
				  "message": "요즘 핫한 미션 목록을 조회했습니다.",
				  "data": {
				    "windowStart": "2026-08-03T00:00:00",
				    "windowEnd": "2026-08-07T00:00:00",
				    "computedAt": "2026-08-07T00:00:00",
				    "nextRefreshAt": "2026-08-11T00:00:00",
				    "missions": [
				      {
				        "missionId": 10,
				        "title": "성산일출봉에서 일출 보기",
				        "description": "새벽에 올라 일출을 인증해보세요.",
				        "difficulty": "NORMAL",
				        "category": "TOURIST_ATTRACTION",
				        "region": "제주",
				        "imageUrl": null,
				        "addedCount": 12
				      }
				    ]
				  }
				}
				""")
		)
	)
	ResponseEntity<SuccessResponse<HotMissionListResponse>> getHotMissions();
}
