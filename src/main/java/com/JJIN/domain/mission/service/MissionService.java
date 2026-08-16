package com.JJIN.domain.mission.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.mission.dto.internal.MissionSearchCondition;
import com.JJIN.domain.mission.dto.internal.MissionSearchResult;
import com.JJIN.domain.mission.dto.response.MissionCardResponse;
import com.JJIN.domain.mission.dto.response.MissionDetailResponse;
import com.JJIN.domain.mission.dto.response.MissionSearchFeedResponse;
import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.MissionTagMapping;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.mission.entity.enums.MissionSortOption;
import com.JJIN.domain.mission.entity.enums.MissionStatus;
import com.JJIN.domain.mission.exception.MissionErrorCode;
import com.JJIN.domain.mission.repository.MissionRepository;
import com.JJIN.domain.mission.repository.MissionTagMappingRepository;
import com.JJIN.domain.mission.repository.UserMissionRepository;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionService {

	private static final int MAX_PAGE_SIZE = 50;

	private final MissionRepository missionRepository;
	private final MissionTagMappingRepository missionTagMappingRepository;
	private final UserMissionRepository userMissionRepository;

	@Transactional(readOnly = true)
	public MissionSearchFeedResponse searchMissions(
		final Long memberId,
		final String keyword,
		final List<TourApiContentType> categories,
		final List<MissionDifficulty> difficulties,
		final String sort,
		final int page,
		final int size
	) {
		validatePageRequest(page, size);
		MissionSortOption sortOption = parseSortOption(sort);
		PageRequest pageRequest = PageRequest.of(page, size);

		MissionSearchCondition condition = new MissionSearchCondition(
			normalizeKeyword(keyword),
			normalizeCategories(categories),
			normalizeDifficulties(difficulties),
			sortOption
		);

		Page<MissionSearchResult> searchResults = missionRepository.search(condition, pageRequest);
		if (searchResults.isEmpty()) {
			return MissionSearchFeedResponse.of(
				new PageImpl<>(List.of(), pageRequest, searchResults.getTotalElements())
			);
		}

		List<Long> missionIds = searchResults.getContent().stream()
			.map(MissionSearchResult::missionId)
			.toList();
		Map<Long, Long> popularityByMissionId = searchResults.getContent().stream()
			.collect(Collectors.toMap(
				MissionSearchResult::missionId,
				MissionSearchResult::popularity,
				(left, right) -> left,
				LinkedHashMap::new
			));
		Map<Long, Mission> missionById = missionRepository.findAllByIdIn(missionIds).stream()
			.collect(Collectors.toMap(Mission::getId, Function.identity()));
		Map<Long, List<String>> tagsByMissionId = getTagsByMissionId(missionIds);
		Set<Long> addedMissionIds = userMissionRepository.findMissionIdsByMemberIdAndMissionIdIn(memberId, missionIds);

		List<MissionCardResponse> cards = missionIds.stream()
			.map(missionId -> MissionCardResponse.of(
				missionById.get(missionId),
				tagsByMissionId.getOrDefault(missionId, List.of()),
				popularityByMissionId.getOrDefault(missionId, 0L),
				addedMissionIds.contains(missionId)
			))
			.toList();

		Page<MissionCardResponse> cardPage = new PageImpl<>(cards, pageRequest, searchResults.getTotalElements());
		return MissionSearchFeedResponse.of(cardPage);
	}

	@Transactional(readOnly = true)
	public MissionDetailResponse getMission(
		final Long memberId,
		final Long missionId
	) {
		Mission mission = missionRepository.findByIdAndStatus(missionId, MissionStatus.ACTIVE)
			.orElseThrow(() -> new JjinException(MissionErrorCode.MISSION_NOT_FOUND));
		List<String> tags = getTagsByMissionId(List.of(missionId)).getOrDefault(missionId, List.of());
		boolean added = userMissionRepository.existsByMemberIdAndMissionId(memberId, missionId);

		return MissionDetailResponse.of(mission, tags, added);
	}

	private Map<Long, List<String>> getTagsByMissionId(final List<Long> missionIds) {
		if (missionIds.isEmpty()) {
			return Map.of();
		}
		return missionTagMappingRepository.findAllByMissionIdInWithTag(missionIds).stream()
			.collect(Collectors.groupingBy(
				mapping -> mapping.getMission().getId(),
				LinkedHashMap::new,
				Collectors.mapping(mapping -> mapping.getTag().getName(), Collectors.toList())
			));
	}

	private MissionSortOption parseSortOption(final String sort) {
		try {
			return MissionSortOption.from(sort);
		} catch (IllegalArgumentException e) {
			throw new JjinException(MissionErrorCode.INVALID_SORT_OPTION);
		}
	}

	private String normalizeKeyword(final String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		return keyword.trim().toLowerCase(Locale.ROOT);
	}

	private List<TourApiContentType> normalizeCategories(final List<TourApiContentType> categories) {
		if (categories == null || categories.isEmpty()) {
			return List.of();
		}
		return categories.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(LinkedHashSet::new))
			.stream()
			.toList();
	}

	private List<MissionDifficulty> normalizeDifficulties(final List<MissionDifficulty> difficulties) {
		if (difficulties == null || difficulties.isEmpty()) {
			return List.of();
		}
		return difficulties.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(LinkedHashSet::new))
			.stream()
			.toList();
	}

	private void validatePageRequest(
		final int page,
		final int size
	) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new JjinException(MissionErrorCode.INVALID_PAGE_REQUEST);
		}
	}
}
