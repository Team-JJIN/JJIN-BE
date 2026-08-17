package com.JJIN.domain.mission.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.member.entity.Member;
import com.JJIN.domain.member.repository.MemberRepository;
import com.JJIN.domain.mission.dto.internal.MissionSearchCondition;
import com.JJIN.domain.mission.dto.internal.MissionSearchResult;
import com.JJIN.domain.mission.dto.request.CreateMissionRequest;
import com.JJIN.domain.mission.dto.response.AddMissionToPlansResponse;
import com.JJIN.domain.mission.dto.response.MissionCardResponse;
import com.JJIN.domain.mission.dto.response.MissionDetailResponse;
import com.JJIN.domain.mission.dto.response.MissionLikeStatusResponse;
import com.JJIN.domain.mission.dto.response.MissionSearchFeedResponse;
import com.JJIN.domain.mission.dto.response.PresignedUrlResponse;
import com.JJIN.domain.mission.entity.HotMissionItem;
import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.MissionTag;
import com.JJIN.domain.mission.entity.MissionTagMapping;
import com.JJIN.domain.mission.entity.UserMission;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.mission.entity.enums.MissionSortOption;
import com.JJIN.domain.mission.entity.enums.MissionSourceType;
import com.JJIN.domain.mission.entity.enums.MissionSourceTypeOption;
import com.JJIN.domain.mission.entity.enums.MissionStatus;
import com.JJIN.domain.mission.exception.MissionErrorCode;
import com.JJIN.domain.mission.repository.HotMissionSnapshotRepository;
import com.JJIN.domain.mission.repository.MissionRepository;
import com.JJIN.domain.mission.repository.MissionTagMappingRepository;
import com.JJIN.domain.mission.repository.MissionTagRepository;
import com.JJIN.domain.mission.repository.UserMissionRepository;
import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.repository.TravelPlanRepository;
import com.JJIN.global.exception.JjinException;
import com.JJIN.global.s3.S3PresignedUrlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionService {

	private static final int MAX_PAGE_SIZE = 50;
	private static final String MISSION_IMAGE_PREFIX = "mission/";
	private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
		"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
	);

	private final MissionRepository missionRepository;
	private final MissionTagMappingRepository missionTagMappingRepository;
	private final UserMissionRepository userMissionRepository;
	private final MemberRepository memberRepository;
	private final MissionTagRepository missionTagRepository;
	private final MissionCategoryClassifier missionCategoryClassifier;
	private final S3PresignedUrlService s3PresignedUrlService;
	private final TravelPlanRepository travelPlanRepository;
	private final HotMissionSnapshotRepository hotMissionSnapshotRepository;

	@Transactional(readOnly = true)
	public MissionSearchFeedResponse searchMissions(
		final Long memberId,
		final String keyword,
		final List<TourApiContentType> categories,
		final List<MissionDifficulty> difficulties,
		final String sort,
		final int page,
		final int size,
		final MissionSourceTypeOption source
	) {
		if (source == MissionSourceTypeOption.HOT) {
			return buildHotMissionsResponse(memberId);
		}
		if (source == MissionSourceTypeOption.ADDED) {
			return buildAddedMissionsResponse(memberId);
		}

		validatePageRequest(page, size);
		MissionSortOption sortOption = parseSortOption(sort);
		PageRequest pageRequest = PageRequest.of(page, size);

		MissionSourceType sourceType = source == MissionSourceTypeOption.OFFICIAL
			? MissionSourceType.OFFICIAL : null;

		MissionSearchCondition condition = new MissionSearchCondition(
			normalizeKeyword(keyword),
			normalizeCategories(categories),
			normalizeDifficulties(difficulties),
			sortOption,
			sourceType
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

	public PresignedUrlResponse createPresignedUrl(final String fileName, final String contentType) {
		if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			throw new JjinException(MissionErrorCode.UNSUPPORTED_IMAGE_TYPE);
		}

		String key = MISSION_IMAGE_PREFIX + UUID.randomUUID() + "_" + fileName;
		String presignedUrl = s3PresignedUrlService.generatePutPresignedUrl(key, contentType);
		return PresignedUrlResponse.of(presignedUrl, key);
	}

	@Transactional
	public Long createMission(final Long memberId, final CreateMissionRequest request) {
		TourApiContentType category = missionCategoryClassifier.classify(
			request.title(), request.description(), request.tags()
		);

		Member createdBy = memberRepository.getReferenceById(memberId);

		Mission mission = missionRepository.save(
			Mission.create(
				request.title(),
				request.description(),
				request.difficulty(),
				category,
				request.imageUrl(),
				createdBy,
				MissionSourceType.USER_CREATED
			)
		);

		saveTags(mission, request.tags());
		return mission.getId();
	}


	@Transactional
	public AddMissionToPlansResponse addMissionToPlans(
		final Long memberId,
		final Long missionId,
		final List<Long> planIds
	) {
		Mission mission = missionRepository.findById(missionId)
			.orElseThrow(() -> new JjinException(MissionErrorCode.MISSION_NOT_FOUND));

		List<AddMissionToPlansResponse.LikeItem> likes = new ArrayList<>();
		for (Long planId : planIds) {
			TravelPlan plan = travelPlanRepository.findById(planId)
				.orElseThrow(() -> new JjinException(MissionErrorCode.TRAVEL_PLAN_NOT_FOUND));
			if (!plan.getMember().getId().equals(memberId)) {
				throw new JjinException(MissionErrorCode.NOT_PLAN_OWNER);
			}
			UserMission userMission = userMissionRepository
				.findByTravelPlanIdAndMissionId(planId, missionId)
				.orElseGet(() -> userMissionRepository.save(UserMission.add(plan.getMember(), mission, plan)));
			likes.add(new AddMissionToPlansResponse.LikeItem(userMission.getId(), planId));
		}
		return AddMissionToPlansResponse.of(likes);
	}

	@Transactional
	public void removeMissionFromPlans(
		final Long memberId,
		final Long missionId,
		final List<Long> planIds
	) {
		for (Long planId : planIds) {
			userMissionRepository.findByTravelPlanIdAndMissionId(planId, missionId)
				.ifPresent(userMission -> {
					if (!userMission.getMember().getId().equals(memberId)) {
						throw new JjinException(MissionErrorCode.NOT_PLAN_OWNER);
					}
					userMissionRepository.delete(userMission);
				});
		}
	}

	@Transactional(readOnly = true)
	public MissionLikeStatusResponse getMissionLikeStatus(final Long memberId, final Long missionId) {
		List<TravelPlan> plans = travelPlanRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

		Map<Long, UserMission> likedByPlanId = userMissionRepository
			.findAllByMemberIdAndMissionId(memberId, missionId)
			.stream()
			.collect(Collectors.toMap(um -> um.getTravelPlan().getId(), um -> um));

		List<MissionLikeStatusResponse.PlanLikeItem> items = plans.stream()
			.map(plan -> {
				UserMission um = likedByPlanId.get(plan.getId());
				return new MissionLikeStatusResponse.PlanLikeItem(
					plan.getId(),
					plan.getName(),
					plan.getStartDate(),
					plan.getEndDate(),
					um != null,
					um != null ? um.getId() : null
				);
			})
			.toList();

		return MissionLikeStatusResponse.of(items);
	}

	private void saveTags(final Mission mission, final List<String> tagNames) {
		if (tagNames == null || tagNames.isEmpty()) {
			return;
		}
		for (String tagName : tagNames) {
			MissionTag tag = missionTagRepository.findByName(tagName)
				.orElseGet(() -> missionTagRepository.save(MissionTag.of(tagName)));
			missionTagMappingRepository.save(MissionTagMapping.of(mission, tag));
		}
	}

	private MissionSearchFeedResponse buildHotMissionsResponse(final Long memberId) {
		List<HotMissionItem> items = hotMissionSnapshotRepository
			.findTopByOrderByComputedAtDesc()
			.map(snapshot -> hotMissionSnapshotRepository.findItemsWithMission(snapshot))
			.orElse(List.of());

		if (items.isEmpty()) {
			return new MissionSearchFeedResponse(List.of(), 0, 0, 0, false);
		}

		List<Long> missionIds = items.stream().map(i -> i.getMission().getId()).toList();
		Map<Long, List<String>> tagsByMissionId = getTagsByMissionId(missionIds);
		Set<Long> addedMissionIds = userMissionRepository.findMissionIdsByMemberIdAndMissionIdIn(memberId, missionIds);

		List<MissionCardResponse> cards = items.stream()
			.map(item -> MissionCardResponse.of(
				item.getMission(),
				tagsByMissionId.getOrDefault(item.getMission().getId(), List.of()),
				item.getAddedCount(),
				addedMissionIds.contains(item.getMission().getId())
			))
			.toList();

		return new MissionSearchFeedResponse(cards, cards.size(), 0, cards.size(), false);
	}

	private MissionSearchFeedResponse buildAddedMissionsResponse(final Long memberId) {
		List<Long> missionIds = userMissionRepository.findDistinctMissionIdsByMemberId(memberId);

		if (missionIds.isEmpty()) {
			return new MissionSearchFeedResponse(List.of(), 0, 0, 0, false);
		}

		Map<Long, Mission> missionById = missionRepository.findAllByIdIn(missionIds).stream()
			.filter(m -> m.getStatus() == MissionStatus.ACTIVE)
			.collect(Collectors.toMap(Mission::getId, Function.identity()));
		Map<Long, List<String>> tagsByMissionId = getTagsByMissionId(new ArrayList<>(missionById.keySet()));

		List<MissionCardResponse> cards = missionById.values().stream()
			.map(mission -> MissionCardResponse.of(
				mission,
				tagsByMissionId.getOrDefault(mission.getId(), List.of()),
				0L,
				true
			))
			.toList();

		return new MissionSearchFeedResponse(cards, cards.size(), 0, cards.size(), false);
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
