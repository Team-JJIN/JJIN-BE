package com.JJIN.domain.place.tourapi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.place.candidate.PlaceCandidate;
import com.JJIN.domain.place.entity.Place;
import com.JJIN.domain.place.entity.PlaceLocalizedContent;
import com.JJIN.domain.place.entity.PlaceOperatingInfo;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.repository.PlaceLocalizedContentRepository;
import com.JJIN.domain.place.repository.PlaceOperatingInfoRepository;
import com.JJIN.domain.place.repository.PlaceRepository;
import com.JJIN.domain.place.tourapi.dto.TourApiIntroItem;
import com.JJIN.domain.place.tourapi.dto.TourApiPlaceItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceSyncService {

	private static final DateTimeFormatter TOUR_API_DATE_TIME =
		DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final DateTimeFormatter TOUR_API_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final PlaceRepository placeRepository;
	private final PlaceLocalizedContentRepository localizedContentRepository;
	private final PlaceOperatingInfoRepository operatingInfoRepository;

	@Transactional
	public List<PlaceCandidate> syncPlaces(
		final PlaceLocale locale,
		final List<TourApiPlaceItem> items
	) {
		List<PlaceCandidate> candidates = new ArrayList<>();
		for (TourApiPlaceItem item : items) {
			syncPlace(locale, item).ifPresent(candidates::add);
		}
		return candidates;
	}

	@Transactional
	public Optional<PlaceCandidate> syncPlace(
		final PlaceLocale locale,
		final TourApiPlaceItem item
	) {
		if (item == null || !StringUtils.hasText(item.contentId())) {
			return Optional.empty();
		}

		Optional<PlaceLocalizedContent> existingLocalized = localizedContentRepository
			.findByLocaleAndExternalContentId(locale, item.contentId());
		TourApiContentType itemContentType = parseContentType(locale, item.contentTypeId()).orElse(null);

		Place place = existingLocalized
			.map(PlaceLocalizedContent::getPlace)
			.orElseGet(() -> findCrosswalkPlace(locale, item, itemContentType).orElse(null));

		BigDecimal longitude = parseDecimal(item.mapx());
		BigDecimal latitude = parseDecimal(item.mapy());
		if (place == null) {
			if (itemContentType == null || longitude == null || latitude == null
				|| !StringUtils.hasText(item.title())) {
				return Optional.empty();
			}
			place = Place.create(
				itemContentType,
				longitude,
				latitude,
				normalize(item.lDongRegnCd()),
				normalize(item.lDongSignguCd()),
				normalize(item.lclsSystm1()),
				normalize(item.lclsSystm2())
			);
			placeRepository.save(place);
		} else {
			place.updateBasicInformation(
				itemContentType == null ? place.getContentType() : itemContentType,
				longitude == null ? place.getLongitude() : longitude,
				latitude == null ? place.getLatitude() : latitude,
				prefer(item.lDongRegnCd(), place.getLegalDongRegionCode()),
				prefer(item.lDongSignguCd(), place.getLegalDongDistrictCode()),
				prefer(item.lclsSystm1(), place.getLclsSystm1Code()),
				prefer(item.lclsSystm2(), place.getLclsSystm2Code())
			);
		}

		if (place.getContentType() == TourApiContentType.FESTIVAL_EVENT) {
			place.updateFestivalInformation(
				parseDate(item.eventstartdate()).orElse(place.getFestivalStartDate()),
				parseDate(item.eventenddate()).orElse(place.getFestivalEndDate()),
				place.getFestivalTimeText(),
				place.getFestivalPlace()
			);
		}

		LocalDateTime now = LocalDateTime.now();
		PlaceLocalizedContent localized = existingLocalized.orElse(null);
		if (localized == null) {
			localized = localizedContentRepository
				.findByPlaceIdAndLocaleAndVisibleTrue(place.getId(), locale)
				.orElse(null);
		}
		if (localized == null) {
			if (!StringUtils.hasText(item.title())) {
				return Optional.empty();
			}
			localized = PlaceLocalizedContent.create(
				place,
				locale,
				item.contentId(),
				item.title().trim(),
				joinAddress(item.addr1(), item.addr2()),
				normalize(item.overview()),
				normalize(item.firstimage()),
				normalize(item.firstimage2()),
				normalize(item.cpyrhtDivCd()),
				parseDateTime(item.modifiedtime()).orElse(null),
				now
			);
			localizedContentRepository.save(localized);
		} else {
			localized.updateContent(
				prefer(item.title(), localized.getName()),
				prefer(joinAddress(item.addr1(), item.addr2()), localized.getAddress()),
				prefer(item.overview(), localized.getOverview()),
				prefer(item.firstimage(), localized.getRepresentativeImageUrl()),
				prefer(item.firstimage2(), localized.getThumbnailImageUrl()),
				prefer(item.cpyrhtDivCd(), localized.getImageCopyrightTypeCode()),
				parseDateTime(item.modifiedtime()).orElse(localized.getExternalModifiedAt()),
				now
			);
			localized.changeVisibility(true);
		}

		return Optional.of(toCandidate(place, localized));
	}

	@Transactional
	public void syncIntro(
		final Long placeId,
		final TourApiContentType contentType,
		final TourApiIntroItem intro
	) {
		if (intro == null) {
			return;
		}
		Place place = placeRepository.findById(placeId)
			.orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다: " + placeId));

		String openingHours = normalize(intro.openingHours(contentType));
		String restDay = normalize(intro.restDay(contentType));
		if (openingHours != null || restDay != null) {
			PlaceOperatingInfo operatingInfo = operatingInfoRepository.findById(placeId)
				.orElseGet(() -> PlaceOperatingInfo.create(place, openingHours, restDay));
			if (operatingInfo.getPlaceId() != null) {
				operatingInfo.updateRawInformation(openingHours, restDay);
			}
			operatingInfoRepository.save(operatingInfo);
		}

		if (contentType == TourApiContentType.FESTIVAL_EVENT) {
			place.updateFestivalInformation(
				parseDate(intro.eventstartdate()).orElse(place.getFestivalStartDate()),
				parseDate(intro.eventenddate()).orElse(place.getFestivalEndDate()),
				prefer(intro.playtime(), place.getFestivalTimeText()),
				prefer(intro.eventplace(), place.getFestivalPlace())
			);
		}
	}

	private Optional<Place> findCrosswalkPlace(
		final PlaceLocale locale,
		final TourApiPlaceItem item,
		final TourApiContentType contentType
	) {
		Optional<Place> sameExternalId = localizedContentRepository
			.findAllByExternalContentId(item.contentId())
			.stream()
			.map(PlaceLocalizedContent::getPlace)
			.filter(place -> contentType == null || place.getContentType() == contentType)
			.findFirst();
		if (sameExternalId.isPresent()) {
			return sameExternalId;
		}

		BigDecimal longitude = parseDecimal(item.mapx());
		BigDecimal latitude = parseDecimal(item.mapy());
		if (contentType == null || longitude == null || latitude == null) {
			return Optional.empty();
		}

		List<Place> matches = placeRepository
			.findAllByContentTypeAndLongitudeAndLatitude(contentType, longitude, latitude)
			.stream()
			.filter(place -> !localizedContentRepository.existsByPlaceIdAndLocale(place.getId(), locale))
			.toList();
		return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
	}

	private PlaceCandidate toCandidate(
		final Place place,
		final PlaceLocalizedContent localized
	) {
		return new PlaceCandidate(
			place.getId(),
			place.getContentType(),
			localized.getName(),
			localized.getAddress(),
			localized.getRepresentativeImageUrl(),
			place.getLongitude(),
			place.getLatitude(),
			place.getLegalDongRegionCode(),
			place.getLegalDongDistrictCode(),
			place.getLclsSystm1Code(),
			place.getLclsSystm2Code(),
			place.getFestivalStartDate(),
			place.getFestivalEndDate(),
			place.getLocalityScore(),
			place.getLocalityLevel()
		);
	}

	private BigDecimal parseDecimal(final String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return new BigDecimal(value.trim());
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private Optional<LocalDateTime> parseDateTime(final String value) {
		if (!StringUtils.hasText(value)) {
			return Optional.empty();
		}
		try {
			return Optional.of(LocalDateTime.parse(value.trim(), TOUR_API_DATE_TIME));
		} catch (DateTimeParseException ignored) {
			return Optional.empty();
		}
	}

	private Optional<LocalDate> parseDate(final String value) {
		if (!StringUtils.hasText(value)) {
			return Optional.empty();
		}
		try {
			return Optional.of(LocalDate.parse(value.trim(), TOUR_API_DATE));
		} catch (DateTimeParseException ignored) {
			return Optional.empty();
		}
	}

	private Optional<TourApiContentType> parseContentType(
		final PlaceLocale locale,
		final String value
	) {
		if (!StringUtils.hasText(value)) {
			return Optional.empty();
		}
		try {
			return TourApiContentType.fromContentTypeId(locale, Integer.parseInt(value.trim()));
		} catch (NumberFormatException ignored) {
			return Optional.empty();
		}
	}

	private String joinAddress(final String address1, final String address2) {
		String first = normalize(address1);
		String second = normalize(address2);
		if (first == null) {
			return second;
		}
		return second == null ? first : first + " " + second;
	}

	private String prefer(final String newValue, final String oldValue) {
		String normalized = normalize(newValue);
		return normalized == null ? oldValue : normalized;
	}

	private String normalize(final String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
