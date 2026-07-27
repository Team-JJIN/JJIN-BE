package com.JJIN.domain.onboarding.validator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.dto.request.ContentTypePreferenceRequest;
import com.JJIN.domain.onboarding.dto.request.OnboardingCompleteRequest;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;
import com.JJIN.domain.onboarding.exception.OnboardingErrorCode;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;

/**
 * 온보딩 요청의 교차 필드 검증을 담당한다.
 * Bean Validation으로 표현할 수 없는 도메인 규칙만 여기서 다룬다.
 */
@Component
@RequiredArgsConstructor
public class OnboardingRequestValidator {

	public static final ZoneId TRAVEL_ZONE = ZoneId.of("Asia/Seoul");

	private static final int MIN_CONTENT_TYPE_COUNT = 2;
	private static final int MAX_CONTENT_TYPE_COUNT = 4;

	private final Clock clock;

	public void validate(final OnboardingCompleteRequest request) {
		validateRegion(request);
		validateTravelDate(request);
		validateActivityTime(request);
		validatePreferences(request);
	}

	/**
	 * 지역 미정 여부와 지역 값의 조합을 검증한다.
	 */
	private void validateRegion(final OnboardingCompleteRequest request) {
		if (request.regionUndecided() == null) {
			throw new JjinException(OnboardingErrorCode.INVALID_REGION_SELECTION);
		}
		boolean undecided = request.isRegionUndecided();
		boolean hasRegion = request.regionId() != null;

		if (undecided == hasRegion) {
			throw new JjinException(OnboardingErrorCode.INVALID_REGION_SELECTION);
		}
	}

	/**
	 * 여행 시작일/종료일을 Asia/Seoul 기준으로 검증한다.
	 */
	private void validateTravelDate(final OnboardingCompleteRequest request) {
		LocalDate startDate = request.startDate();
		LocalDate endDate = request.endDate();

		if (startDate == null || endDate == null) {
			throw new JjinException(OnboardingErrorCode.INVALID_TRAVEL_DATE);
		}
		if (startDate.isBefore(today())) {
			throw new JjinException(OnboardingErrorCode.INVALID_TRAVEL_DATE);
		}
		if (endDate.isBefore(startDate)) {
			throw new JjinException(OnboardingErrorCode.INVALID_TRAVEL_DATE);
		}
	}

	private void validateActivityTime(final OnboardingCompleteRequest request) {
		if (request.activityStartTime() == null || request.activityEndTime() == null) {
			throw new JjinException(OnboardingErrorCode.INVALID_ACTIVITY_TIME);
		}
		if (!request.activityStartTime().isBefore(request.activityEndTime())) {
			throw new JjinException(OnboardingErrorCode.INVALID_ACTIVITY_TIME);
		}
	}

	/**
	 * TourAPI 관광타입 개수/중복/선택 가능 여부, 세부 취향 존재·중복·소속 관계를 검증한다.
	 */
	private void validatePreferences(final OnboardingCompleteRequest request) {
		List<ContentTypePreferenceRequest> preferences = request.preferences();

		if (preferences == null
			|| preferences.size() < MIN_CONTENT_TYPE_COUNT
			|| preferences.size() > MAX_CONTENT_TYPE_COUNT) {
			throw new JjinException(OnboardingErrorCode.INVALID_CONTENT_TYPE_COUNT);
		}

		Set<TourApiContentType> contentTypes = new HashSet<>();
		Set<TravelSubcategory> seenSubcategories = new HashSet<>();

		for (ContentTypePreferenceRequest preference : preferences) {
			if (preference == null) {
				throw new JjinException(OnboardingErrorCode.INVALID_CONTENT_TYPE_COUNT);
			}

			TourApiContentType contentType = preference.contentType();
			if (contentType == null || !contentType.isPreferenceSelectable()) {
				throw new JjinException(OnboardingErrorCode.INVALID_CONTENT_TYPE_COUNT);
			}
			if (!contentTypes.add(contentType)) {
				throw new JjinException(OnboardingErrorCode.INVALID_CONTENT_TYPE_COUNT);
			}

			List<TravelSubcategory> subcategories = preference.subcategories();
			if (subcategories == null || subcategories.isEmpty()) {
				throw new JjinException(OnboardingErrorCode.MISSING_SUBCATEGORY);
			}

			for (TravelSubcategory subcategory : subcategories) {
				if (subcategory == null || !subcategory.belongsTo(contentType)) {
					throw new JjinException(OnboardingErrorCode.INVALID_SUBCATEGORY);
				}
				if (!seenSubcategories.add(subcategory)) {
					throw new JjinException(OnboardingErrorCode.INVALID_SUBCATEGORY);
				}
			}

			validateAllFoodSelection(contentType, subcategories);
		}
	}

	/**
	 * LIKE_ALL_FOOD는 RESTAURANT 타입 안에서 단독으로만 선택할 수 있다.
	 */
	private void validateAllFoodSelection(
		final TourApiContentType contentType,
		final List<TravelSubcategory> subcategories
	) {
		if (contentType != TourApiContentType.RESTAURANT) {
			return;
		}
		if (subcategories.contains(TravelSubcategory.LIKE_ALL_FOOD) && subcategories.size() > 1) {
			throw new JjinException(OnboardingErrorCode.INVALID_ALL_FOOD_SELECTION);
		}
	}

	private LocalDate today() {
		return LocalDate.now(clock.withZone(TRAVEL_ZONE));
	}
}
