package com.JJIN.domain.mission.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.mission.dto.response.TravelPlanMissionItemResponse;
import com.JJIN.domain.mission.dto.response.TravelPlanMissionListResponse;
import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.enums.MissionDifficulty;
import com.JJIN.domain.mission.entity.enums.MissionStatus;
import com.JJIN.domain.mission.repository.MissionRepository;
import com.JJIN.domain.mission.repository.MissionTagMappingRepository;
import com.JJIN.domain.mission.repository.UserMissionRepository;
import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.onboarding.entity.TravelPreference;
import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.domain.onboarding.repository.TravelPlanRepository;
import com.JJIN.domain.place.entity.PlaceLocalizedContent;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.repository.PlaceLocalizedContentRepository;
import com.JJIN.domain.place.repository.PlaceRepository;
import com.JJIN.domain.travelplan.exception.TravelPlanErrorCode;
import com.JJIN.domain.travelplan.repository.TravelCourseStopRepository;
import com.JJIN.global.ai.RunyourAiChatClient;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * AI 미션 추천.
 * DB에서 취향·인기 신호로 후보를 top-N으로 좁힌 뒤,
 * 그 목록만 LLM에 넘겨 사용자의 코스·취향에 가장 잘 맞는 미션을 최종 선별한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMissionRecommendationService {

	// 선택 취향 카테고리에서 많이, 미선택 카테고리에서 소량을 shortlist로 뽑아 LLM에 넘긴다.
	private static final int SELECTED_SHORTLIST = 25;
	private static final int UNSELECTED_SHORTLIST = 10;
	private static final int RECOMMEND_COUNT = 6;

	private static final String SYSTEM_PROMPT = """
		너는 여행자에게 어울리는 미션을 추천하는 전문가다.
		제공된 후보 미션 목록(id 포함) 중에서 사용자의 여행 코스와 취향에 가장 잘 맞는 미션 %d개를 고른다.

		[규칙]
		1. 반드시 제공된 미션 id 중에서만 고른다. 목록에 없는 미션을 만들지 않는다.
		2. 한강 불꽃축제, 워터밤 같이 특정 기간(시즌)에만 하는 미션은, 여행 기간이 그 시즌 근처일 때만 추천한다.
		   (예: 벚꽃축제=봄, 워터밤·물축제=여름, 불꽃축제=가을. 여행 월과 시즌이 맞지 않으면 제외한다.)
		3. 사용자가 선택한 취향 카테고리를 우선하되, 코스에 재미와 다양성을 더하는 다른 카테고리 미션도 일부 섞는다.
		4. 지역 제약(매우 중요): 각 미션이 실제로 '어느 도시/지역'에서 하는 것인지 너의 지식으로 판단하라.
		   제목·설명에 지역명이 적혀 있지 않아도, 특정 장소·명소에 묶인 미션이면 그 장소가 있는 도시를 추론한다.
		   - 서울: 광장시장, 국립중앙박물관(사유의 방), 경복궁, 남산/N서울타워, 북촌한옥마을, 청계천, 한강, 서울스카이, 성수동, 별마당도서관, 여의도
		   - 부산: 광안리, 감천문화마을, 해운대  / 전주: 전주 한옥마을  / 강원: 양양 서핑  / 진주: 남강 유등축제
		   위처럼 특정 도시에 묶인 미션은, 그 도시가 사용자 여행 지역(및 서울·인천 같은 인접 수도권)과 다르면 절대 추천하지 않는다.
		   전국 어디서나 할 수 있는 미션(편의점 조합, 다이소 쇼핑, 마라탕후루, 붕어빵/호떡, 코인노래방, 찜질방, 방탈출, 스크린골프 등)만
		   지역과 무관하게 추천할 수 있다.
		   예: 여행지가 부산이면 광장시장·국립중앙박물관·경복궁(서울)은 금지 / 여행지가 서울이면 광안리·감천·전주·양양은 금지.
		5. 결과는 미션 id의 JSON 배열로만 출력한다. 다른 텍스트·설명·마크다운을 절대 포함하지 않는다. 예: [7, 12, 3]
		""".formatted(RECOMMEND_COUNT);

	private final TravelPlanRepository travelPlanRepository;
	private final MissionRepository missionRepository;
	private final MissionTagMappingRepository missionTagMappingRepository;
	private final UserMissionRepository userMissionRepository;
	private final TravelCourseStopRepository courseStopRepository;
	private final PlaceRepository placeRepository;
	private final PlaceLocalizedContentRepository localizedContentRepository;
	private final RunyourAiChatClient runyourAiChatClient;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public TravelPlanMissionListResponse recommend(final Long memberId, final Long travelPlanId) {
		TravelPlan plan = travelPlanRepository.findById(travelPlanId)
			.orElseThrow(() -> new JjinException(TravelPlanErrorCode.TRAVEL_PLAN_NOT_FOUND));
		if (!plan.getMember().getId().equals(memberId)) {
			throw new JjinException(TravelPlanErrorCode.TRAVEL_PLAN_FORBIDDEN);
		}

		Set<TourApiContentType> preferred = plan.getPreferences().stream()
			.map(TravelPreference::getContentType)
			.collect(Collectors.toSet());

		List<String> coursePlaces = buildCoursePlaceLabels(travelPlanId);

		// 코스가 비어 있으면 추천할 기준이 없으므로, LLM 호출 없이 미션 목록을 null로 반환한다.
		if (coursePlaces.isEmpty()) {
			return TravelPlanMissionListResponse.of(plan, 0, 0, 0, 0, null);
		}

		// 이미 이 일정에 배정된 미션은 후보에서 제외
		Set<Long> assigned = userMissionRepository.findAllByTravelPlanIdOrderByAddedAtDescIdDesc(travelPlanId)
			.stream().map(um -> um.getMission().getId()).collect(Collectors.toSet());

		List<Mission> candidates = missionRepository.findAllByStatus(MissionStatus.ACTIVE).stream()
			.filter(m -> !assigned.contains(m.getId()))
			.toList();

		List<Mission> shortlist = shortlist(candidates, preferred, plan.getExperienceLevel());
		if (shortlist.isEmpty()) {
			return TravelPlanMissionListResponse.of(plan, 0, 0, 0, 0, List.of());
		}

		Map<Long, List<String>> tagsByMission = loadTags(shortlist);

		String userPrompt = buildUserPrompt(plan, preferred, coursePlaces, shortlist, tagsByMission);
		Set<Long> whitelist = shortlist.stream().map(Mission::getId).collect(Collectors.toSet());

		List<Long> chosenIds;
		try {
			String raw = runyourAiChatClient.chat(SYSTEM_PROMPT, userPrompt);
			log.info("[AI미션추천] LLM 원응답: {}", raw);
			chosenIds = parseIds(raw, whitelist);
		} catch (RuntimeException exception) {
			log.error("AI 미션 추천 LLM 호출 실패. 상위 후보로 폴백합니다.", exception);
			chosenIds = List.of();
		}
		if (chosenIds.isEmpty()) {
			chosenIds = shortlist.stream().map(Mission::getId).limit(RECOMMEND_COUNT).toList();
		}

		Map<Long, Mission> missionById = shortlist.stream()
			.collect(Collectors.toMap(Mission::getId, Function.identity(), (a, b) -> a));
		List<TravelPlanMissionItemResponse> items = chosenIds.stream()
			.map(missionById::get)
			.filter(Objects::nonNull)
			.map(m -> TravelPlanMissionItemResponse.ofRecommendation(
				m, tagsByMission.getOrDefault(m.getId(), List.of())))
			.toList();

		return TravelPlanMissionListResponse.of(plan, items.size(), 0, 0, 0, items);
	}

	/** 선택 취향 버킷 top-N + 미선택 버킷 top-N 을 합쳐 shortlist를 만든다. */
	private List<Mission> shortlist(
		final List<Mission> candidates,
		final Set<TourApiContentType> preferred,
		final ExperienceLevel level
	) {
		Map<Boolean, List<Mission>> byBucket = candidates.stream()
			.collect(Collectors.partitioningBy(
				m -> m.getCategory() != null && preferred.contains(m.getCategory())));

		List<Mission> result = new ArrayList<>(topN(byBucket.get(true), level, SELECTED_SHORTLIST));
		result.addAll(topN(byBucket.get(false), level, UNSELECTED_SHORTLIST));
		return result;
	}

	/** 난이도가 여행 레벨에 가까운 순으로 정렬해 상위 N개만 남긴다. */
	private List<Mission> topN(final List<Mission> missions, final ExperienceLevel level, final int limit) {
		return missions.stream()
			.sorted((a, b) -> Integer.compare(
				difficultyGap(level, a.getDifficulty()), difficultyGap(level, b.getDifficulty())))
			.limit(limit)
			.toList();
	}

	private int difficultyGap(final ExperienceLevel level, final MissionDifficulty difficulty) {
		int target = switch (level) {
			case LIGHT -> 1;
			case NORMAL -> 2;
			case DEEP -> 3;
		};
		int value = switch (difficulty) {
			case ONE -> 1;
			case TWO -> 2;
			case THREE -> 3;
		};
		return Math.abs(target - value);
	}

	private Map<Long, List<String>> loadTags(final List<Mission> shortlist) {
		List<Long> ids = shortlist.stream().map(Mission::getId).toList();
		return missionTagMappingRepository.findAllByMissionIdInWithTag(ids).stream()
			.collect(Collectors.groupingBy(
				mapping -> mapping.getMission().getId(),
				Collectors.mapping(mapping -> mapping.getTag().getName(), Collectors.toList())));
	}

	private List<String> buildCoursePlaceLabels(final Long travelPlanId) {
		List<Long> placeIds = courseStopRepository.findPlaceIdsByTravelPlanId(travelPlanId);
		if (placeIds.isEmpty()) {
			return List.of();
		}
		Map<Long, String> nameById = localizedContentRepository
			.findAllByPlaceIdInAndLocaleAndVisibleTrue(placeIds, PlaceLocale.KO).stream()
			.collect(Collectors.toMap(c -> c.getPlace().getId(), PlaceLocalizedContent::getName, (a, b) -> a));
		return placeRepository.findAllById(placeIds).stream()
			.map(p -> nameById.getOrDefault(p.getId(), "장소") + "(" + p.getContentType().getDisplayName() + ")")
			.toList();
	}

	private String buildUserPrompt(
		final TravelPlan plan,
		final Set<TourApiContentType> preferred,
		final List<String> coursePlaces,
		final List<Mission> shortlist,
		final Map<Long, List<String>> tagsByMission
	) {
		String preferredLabels = preferred.isEmpty() ? "(선택 없음)"
			: preferred.stream().map(TourApiContentType::getDisplayName).collect(Collectors.joining(", "));
		String courseText = coursePlaces.isEmpty() ? "(아직 코스 없음)" : String.join(", ", coursePlaces);

		StringBuilder sb = new StringBuilder();
		sb.append("[여행 조건]\n");
		sb.append("- 지역: ").append(plan.getRegion() == null ? "미정" : plan.getRegion().getDisplayName()).append("\n");
		sb.append("- 여행 기간: ").append(plan.getStartDate()).append(" ~ ").append(plan.getEndDate()).append("\n");
		sb.append("- 여행 레벨: ").append(plan.getExperienceLevel().getDisplayName()).append("\n");
		sb.append("- 선택 취향 카테고리: ").append(preferredLabels).append("\n");
		sb.append("- 코스 방문지: ").append(courseText).append("\n\n");

		sb.append("[후보 미션 - id | 카테고리 | 난이도 | 태그 | 제목 - 설명]\n");
		for (Mission m : shortlist) {
			String tags = tagsByMission.getOrDefault(m.getId(), List.of()).stream()
				.collect(Collectors.joining(","));
			sb.append("- ").append(m.getId())
				.append(" | ").append(m.getCategory() == null ? "미분류" : m.getCategory().getDisplayName())
				.append(" | ").append(m.getDifficulty())
				.append(" | [").append(tags).append("]")
				.append(" | ").append(m.getTitle()).append(" - ").append(m.getDescription())
				.append("\n");
		}
		sb.append("\n위 후보 중 가장 잘 맞는 ").append(RECOMMEND_COUNT)
			.append("개를 골라 미션 id만 JSON 배열로 출력하라.");
		return sb.toString();
	}

	/** LLM 응답에서 미션 id 배열을 파싱하고, shortlist 안에 있는 id만 순서대로 남긴다. */
	private List<Long> parseIds(final String raw, final Set<Long> whitelist) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		String json = stripCodeFence(raw.trim());
		try {
			Long[] ids = objectMapper.readValue(json, Long[].class);
			List<Long> result = new ArrayList<>();
			for (Long id : ids) {
				if (id != null && whitelist.contains(id) && !result.contains(id)) {
					result.add(id);
				}
				if (result.size() >= RECOMMEND_COUNT) {
					break;
				}
			}
			return result;
		} catch (RuntimeException exception) {
			log.warn("AI 미션 추천 응답 파싱 실패: '{}'", raw);
			return List.of();
		}
	}

	private String stripCodeFence(final String text) {
		if (!text.startsWith("```")) {
			return text;
		}
		int firstNewline = text.indexOf('\n');
		String body = firstNewline < 0 ? text : text.substring(firstNewline + 1);
		if (body.endsWith("```")) {
			body = body.substring(0, body.length() - 3);
		}
		return body.strip();
	}
}
