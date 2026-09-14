package com.JJIN.domain.recommendation.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.entity.enums.TourApiClassification;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;

/**
 * 사용자가 선택한 세부 취향(TravelSubcategory)에서 CategoryFit 판정용 중분류(lclsSystm2) 코드 집합을 추출한다.
 */
@Component
public class PreferredCategoryResolver {

	public Set<String> resolveMidCategoryCodes(final Collection<TravelSubcategory> subcategories) {
		if (subcategories == null || subcategories.isEmpty()) {
			return Set.of();
		}
		return subcategories.stream()
			.filter(Objects::nonNull)
			.flatMap(subcategory -> subcategory.getClassifications().stream())
			.map(TourApiClassification::lclsSystm2)
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
