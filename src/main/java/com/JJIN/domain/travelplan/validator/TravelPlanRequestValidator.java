package com.JJIN.domain.travelplan.validator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;
import com.JJIN.domain.travelplan.dto.internal.CreateTravelPlanCommand;
import com.JJIN.domain.travelplan.exception.TravelPlanErrorCode;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TravelPlanRequestValidator {

	public static final ZoneId TRAVEL_ZONE = ZoneId.of("Asia/Seoul");

	private static final int MIN_CONTENT_TYPE_COUNT = 2;
	private static final int MAX_CONTENT_TYPE_COUNT = 4;

	private final Clock clock;

	public void validateCreate(final CreateTravelPlanCommand command) {
		validateRegion(command);
		validateTravelDate(command);
		validateActivityTime(command);
		validatePreferences(command.preferences());
	}

	private void validateRegion(final CreateTravelPlanCommand command) {
		if (command.regionUndecided() == null) {
			throw new JjinException(TravelPlanErrorCode.INVALID_REGION_SELECTION);
		}
		boolean hasRegion = command.regionId() != null;
		if (command.isRegionUndecided() == hasRegion) {
			throw new JjinException(TravelPlanErrorCode.INVALID_REGION_SELECTION);
		}
	}

	private void validateTravelDate(final CreateTravelPlanCommand command) {
		LocalDate startDate = command.startDate();
		LocalDate endDate = command.endDate();
		if (startDate == null || endDate == null
			|| startDate.isBefore(LocalDate.now(clock.withZone(TRAVEL_ZONE)))
			|| endDate.isBefore(startDate)) {
			throw new JjinException(TravelPlanErrorCode.INVALID_TRAVEL_DATE);
		}
	}

	private void validateActivityTime(final CreateTravelPlanCommand command) {
		if (command.activityStartTime() == null || command.activityEndTime() == null
			|| !command.activityStartTime().isBefore(command.activityEndTime())) {
			throw new JjinException(TravelPlanErrorCode.INVALID_ACTIVITY_TIME);
		}
	}

	private void validatePreferences(final List<CreateTravelPlanCommand.Preference> preferences) {
		if (preferences == null
			|| preferences.size() < MIN_CONTENT_TYPE_COUNT
			|| preferences.size() > MAX_CONTENT_TYPE_COUNT) {
			throw new JjinException(TravelPlanErrorCode.INVALID_CONTENT_TYPE_COUNT);
		}

		Set<TourApiContentType> contentTypes = new HashSet<>();
		Set<TravelSubcategory> seenSubcategories = new HashSet<>();
		for (CreateTravelPlanCommand.Preference preference : preferences) {
			validatePreference(preference, contentTypes, seenSubcategories);
		}
	}

	private void validatePreference(
		final CreateTravelPlanCommand.Preference preference,
		final Set<TourApiContentType> contentTypes,
		final Set<TravelSubcategory> seenSubcategories
	) {
		if (preference == null || preference.contentType() == null
			|| !preference.contentType().isPreferenceSelectable()
			|| !contentTypes.add(preference.contentType())) {
			throw new JjinException(TravelPlanErrorCode.INVALID_CONTENT_TYPE_COUNT);
		}

		List<TravelSubcategory> subcategories = preference.subcategories();
		if (subcategories == null || subcategories.isEmpty()) {
			throw new JjinException(TravelPlanErrorCode.MISSING_SUBCATEGORY);
		}
		for (TravelSubcategory subcategory : subcategories) {
			if (subcategory == null || !subcategory.belongsTo(preference.contentType())
				|| !seenSubcategories.add(subcategory)) {
				throw new JjinException(TravelPlanErrorCode.INVALID_SUBCATEGORY);
			}
		}
		if (preference.contentType() == TourApiContentType.RESTAURANT
			&& subcategories.contains(TravelSubcategory.LIKE_ALL_FOOD)
			&& subcategories.size() > 1) {
			throw new JjinException(TravelPlanErrorCode.INVALID_ALL_FOOD_SELECTION);
		}
	}
}
