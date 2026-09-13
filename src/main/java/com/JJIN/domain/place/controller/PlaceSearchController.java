package com.JJIN.domain.place.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.JJIN.domain.place.controller.docs.PlaceSearchControllerDocs;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.exception.PlaceSuccessCode;
import com.JJIN.domain.place.search.PlaceSearchResponse;
import com.JJIN.domain.place.search.PlaceSearchService;
import com.JJIN.global.auth.annotation.CurrentMember;
import com.JJIN.global.auth.dto.CurrentAuth;
import com.JJIN.global.auth.jwt.exception.TokenErrorCode;
import com.JJIN.global.exception.JjinException;
import com.JJIN.global.response.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceSearchController implements PlaceSearchControllerDocs {

	private final PlaceSearchService placeSearchService;

	@Override
	@GetMapping("/search")
	public ResponseEntity<SuccessResponse<PlaceSearchResponse>> searchPlaces(
		@CurrentMember CurrentAuth currentAuth,
		@RequestParam(name = "keyword") String keyword,
		@RequestParam(name = "locale", defaultValue = "KO") PlaceLocale locale,
		@RequestParam(name = "page", defaultValue = "1") int page,
		@RequestParam(name = "size", defaultValue = "10") int size,
		@RequestParam(name = "latitude", required = false) BigDecimal latitude,
		@RequestParam(name = "longitude", required = false) BigDecimal longitude,
		@RequestParam(name = "planId", required = false) Long planId
	) {
		if (currentAuth == null) {
			throw new JjinException(TokenErrorCode.INVALID_AUTHORIZATION_HEADER);
		}

		return ResponseEntity.ok(
			SuccessResponse.of(
				PlaceSuccessCode.PLACE_SEARCH_SUCCESS,
				placeSearchService.search(keyword, locale, page, size, latitude, longitude, planId)
			)
		);
	}
}
