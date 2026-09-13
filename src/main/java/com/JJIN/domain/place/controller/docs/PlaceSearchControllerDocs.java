package com.JJIN.domain.place.controller.docs;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;

import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.search.PlaceSearchResponse;
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

@Tag(name = "Place", description = "장소 API")
public interface PlaceSearchControllerDocs {

	@Operation(
		summary = "장소 키워드 검색",
		description = """
			TourAPI 키워드 검색으로 장소를 조회한다. locale에 따라 국문(KO)·영문(EN)·일문(JA) 검색을 지원한다.
			검색 결과는 서버 DB에 동기화·상세 보강된 뒤 placeId·운영시간·운영상태와 함께 반환되므로,
			반환된 placeId를 코스 방문지 추가 API에 바로 사용할 수 있다.
			latitude/longitude(브라우저 Geolocation)를 함께 보내면 각 장소까지의 직선거리(m)가 포함되고
			가까운 순으로 정렬된다. 미제공 시 거리 필드는 null이며 TourAPI 기본(가나다) 정렬을 따른다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "장소 검색 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "status": 200,
					  "message": "장소를 검색했습니다.",
					  "data": {
					    "totalCount": 12,
					    "page": 1,
					    "size": 10,
					    "places": [
					      {
					        "placeId": 15,
					        "category": "TOURIST_ATTRACTION",
					        "name": "경복궁",
					        "address": "서울특별시 종로구 사직로 161 (세종로)",
					        "latitude": 37.57861,
					        "longitude": 126.97723,
					        "representativeImageUrl": "https://...",
					        "openTime": "09:00",
					        "closeTime": "18:00",
					        "openStatus": "OPEN",
					        "distanceMeters": 180
					      }
					    ]
					  }
					}
					""")
			)
		),
		@ApiResponse(responseCode = "400", description = "검색어가 비어 있음"),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음")
	})
	ResponseEntity<SuccessResponse<PlaceSearchResponse>> searchPlaces(
		CurrentAuth currentAuth,
		@Parameter(description = "검색 키워드", example = "경복궁") String keyword,
		@Parameter(description = "검색 언어 (KO, EN, JA). 기본값 KO", example = "KO") PlaceLocale locale,
		@Parameter(description = "페이지 번호. 기본값 1", example = "1") int page,
		@Parameter(description = "페이지 크기. 기본값 10", example = "10") int size,
		@Parameter(description = "사용자 위도 (브라우저 Geolocation). 선택", example = "37.5765") BigDecimal latitude,
		@Parameter(description = "사용자 경도 (브라우저 Geolocation). 선택", example = "126.9769") BigDecimal longitude
	);
}
