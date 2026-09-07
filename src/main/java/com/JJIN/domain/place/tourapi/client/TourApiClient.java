package com.JJIN.domain.place.tourapi.client;

import java.util.Optional;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.tourapi.dto.TourApiIntroItem;
import com.JJIN.domain.place.tourapi.dto.TourApiPage;
import com.JJIN.domain.place.tourapi.dto.TourApiPlaceItem;
import com.JJIN.domain.place.tourapi.query.AreaPlaceQuery;
import com.JJIN.domain.place.tourapi.query.FestivalQuery;

public interface TourApiClient {

	TourApiPage<TourApiPlaceItem> getAreaPlaces(PlaceLocale locale, AreaPlaceQuery query);

	TourApiPage<TourApiPlaceItem> getFestivals(PlaceLocale locale, FestivalQuery query);

	Optional<TourApiPlaceItem> getCommonDetail(PlaceLocale locale, String contentId);

	Optional<TourApiIntroItem> getIntroDetail(
		PlaceLocale locale,
		String contentId,
		TourApiContentType contentType
	);
}
