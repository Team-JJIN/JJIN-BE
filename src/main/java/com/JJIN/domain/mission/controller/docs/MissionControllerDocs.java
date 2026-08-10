package com.JJIN.domain.mission.controller.docs;

import org.springframework.http.ResponseEntity;

import com.JJIN.domain.mission.dto.response.HotMissionListResponse;
import com.JJIN.global.response.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Mission", description = "미션 API")
public interface MissionControllerDocs {

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
