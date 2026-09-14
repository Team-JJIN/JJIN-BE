package com.JJIN.domain.place.tourapi.service;

import org.springframework.stereotype.Service;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.entity.PlaceLocalizedContent;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.repository.PlaceLocalizedContentRepository;
import com.JJIN.domain.place.repository.PlaceRepository;
import com.JJIN.domain.place.tourapi.client.CachedTourApiGateway;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourApiPlaceDetailService {

	private final CachedTourApiGateway tourApiGateway;
	private final PlaceSyncService placeSyncService;
	private final PlaceRepository placeRepository;
	private final PlaceLocalizedContentRepository localizedContentRepository;

	/**
	 * 필터·스코어링 이후 남은 상위 후보에 대해서만 호출한다.
	 */
	public void enrich(final Long placeId, final PlaceLocale locale) {
		TourApiContentType contentType = placeRepository.findById(placeId)
			.orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다: " + placeId))
			.getContentType();
		PlaceLocalizedContent identifier = findIdentifier(placeId, locale);
		PlaceLocale sourceLocale = identifier.getLocale();

		tourApiGateway
			.getCommonDetail(sourceLocale, identifier.getExternalContentId())
			.ifPresent(item -> placeSyncService.syncPlace(sourceLocale, item));
		if (contentType.supports(sourceLocale)) {
			tourApiGateway
				.getIntroDetail(sourceLocale, identifier.getExternalContentId(), contentType)
				.ifPresent(intro -> placeSyncService.syncIntro(placeId, contentType, intro));
		}
	}

	private PlaceLocalizedContent findIdentifier(final Long placeId, final PlaceLocale locale) {
		return localizedContentRepository.findByPlaceIdAndLocaleAndVisibleTrue(placeId, locale)
			.or(() -> localizedContentRepository.findByPlaceIdAndLocaleAndVisibleTrue(placeId, PlaceLocale.KO))
			.or(() -> localizedContentRepository.findFirstByPlaceIdAndVisibleTrue(placeId))
			.orElseThrow(() -> new IllegalArgumentException("TourAPI 식별자가 없는 장소입니다: " + placeId));
	}
}
