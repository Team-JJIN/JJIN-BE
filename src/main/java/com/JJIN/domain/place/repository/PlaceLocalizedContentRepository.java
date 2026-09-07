package com.JJIN.domain.place.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.JJIN.domain.place.entity.PlaceLocalizedContent;
import com.JJIN.domain.place.entity.enums.PlaceLocale;

public interface PlaceLocalizedContentRepository extends JpaRepository<PlaceLocalizedContent, Long> {

	Optional<PlaceLocalizedContent> findByLocaleAndExternalContentId(
		PlaceLocale locale,
		String externalContentId
	);

	List<PlaceLocalizedContent> findAllByExternalContentId(String externalContentId);

	Optional<PlaceLocalizedContent> findByPlaceIdAndLocaleAndVisibleTrue(Long placeId, PlaceLocale locale);

	Optional<PlaceLocalizedContent> findFirstByPlaceIdAndVisibleTrue(Long placeId);

	boolean existsByPlaceIdAndLocale(Long placeId, PlaceLocale locale);

	List<PlaceLocalizedContent> findAllByPlaceIdInAndLocaleAndVisibleTrue(
		Collection<Long> placeIds,
		PlaceLocale locale
	);

	List<PlaceLocalizedContent> findAllByPlaceIdInAndVisibleTrue(Collection<Long> placeIds);
}
