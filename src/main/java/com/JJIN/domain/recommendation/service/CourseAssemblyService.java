package com.JJIN.domain.recommendation.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.JJIN.domain.place.entity.PlaceLocalizedContent;
import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.domain.place.repository.PlaceLocalizedContentRepository;
import com.JJIN.domain.recommendation.dto.CourseDraft;
import com.JJIN.domain.recommendation.dto.RecommendationCandidate;
import com.JJIN.domain.recommendation.dto.ScoredCandidate;
import com.JJIN.domain.recommendation.dto.TravelProfile;
import com.JJIN.global.ai.RunyourAiChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * LLM 코스 조립(파이프라인 P4).
 * 스코어링된 상위 후보를 프롬프트로 변환해 LLM에 전달하고, 일자별 코스 초안을 반환한다.
 * LLM은 전달받은 placeId 목록 안에서만 방문지를 선택하며, 검증·보정은 별도 단계(P5)에서 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseAssemblyService {

	private static final String SYSTEM_PROMPT = """
		너는 여행 코스를 설계하는 전문가다.
		제공된 후보 장소 목록(placeId 포함)을 보고, 사용자 조건에 맞는 일자별 방문 코스를 짠다.

		[규칙]
		1. 반드시 제공된 placeId 목록 안에서만 장소를 선택한다. 목록 밖의 장소를 만들거나 추가하지 않는다.
		2. 같은 카테고리(예: 음식점→음식점, 카페→카페)를 연속 배치하지 않는다.
		3. 각 날의 방문은 활동 시작 시각 이후에 시작하고, 활동 종료 시각 이전에 끝나야 한다.
		4. reason은 100자 이내로 간결하게, 한국어로 작성한다.
		5. stayMinutes는 제공된 예상 체류시간을 참고하되, 동선 흐름에 맞게 조정할 수 있다.
		6. 반드시 아래 JSON 형식만 출력한다. 다른 텍스트, 설명, 마크다운 블록을 절대 포함하지 않는다.

		[출력 형식]
		{
		  "days": [
		    {
		      "dayNumber": 1,
		      "visits": [
		        {
		          "placeId": 123,
		          "suggestedStartTime": "09:30",
		          "suggestedEndTime": "11:00",
		          "stayMinutes": 90,
		          "reason": "이 장소를 추천하는 이유"
		        }
		      ]
		    }
		  ]
		}
		""";

	private final PlaceLocalizedContentRepository localizedContentRepository;
	private final RunyourAiChatClient aiChatClient;
	private final ObjectMapper objectMapper;

	/**
	 * 스코어링된 후보로 LLM 코스 초안을 생성한다.
	 *
	 * @param scored   역할별 상위 후보 (P3 산출물)
	 * @param profile  여행 프로파일 (P0 산출물)
	 * @param tripStart 여행 시작일
	 * @param tripEnd   여행 종료일
	 * @return LLM이 생성한 코스 초안. 파싱 실패 시 null 반환(호출부에서 폴백 처리)
	 */
	public CourseDraft assemble(
		final List<ScoredCandidate> scored,
		final TravelProfile profile,
		final LocalDate tripStart,
		final LocalDate tripEnd
	) {
		if (scored.isEmpty()) {
			return null;
		}

		// 표시명 조회 (KO 우선, 없으면 가장 먼저 찾은 언어)
		List<Long> placeIds = scored.stream().map(s -> s.candidate().placeId()).toList();
		Map<Long, String> nameByPlaceId = fetchNames(placeIds);

		String userPrompt = buildUserPrompt(scored, nameByPlaceId, profile, tripStart, tripEnd);
		log.debug("LLM 코스 조립 요청: candidates={}, days={}",
			scored.size(), profile.tripDays());

		try {
			String raw = aiChatClient.chat(SYSTEM_PROMPT, userPrompt);
			CourseDraft draft = objectMapper.readValue(raw, CourseDraft.class);

			// placeId 화이트리스트 검증
			Set<Long> whitelist = placeIds.stream().collect(Collectors.toSet());
			CourseDraft validated = validateWhitelist(draft, whitelist);
			log.info("LLM 코스 초안 생성 완료: days={}, totalVisits={}",
				validated.days().size(),
				validated.days().stream().mapToInt(d -> d.visits().size()).sum());
			return validated;

		} catch (Exception e) {
			log.error("LLM 코스 조립 실패. 폴백으로 처리합니다.", e);
			return null;
		}
	}

	private Map<Long, String> fetchNames(final List<Long> placeIds) {
		List<PlaceLocalizedContent> contents =
			localizedContentRepository.findAllByPlaceIdInAndLocaleAndVisibleTrue(placeIds, PlaceLocale.KO);
		Map<Long, String> nameMap = contents.stream()
			.collect(Collectors.toMap(c -> c.getPlace().getId(), PlaceLocalizedContent::getName));

		// KO가 없는 placeId는 visible=true 아무 언어로 폴백
		placeIds.stream()
			.filter(id -> !nameMap.containsKey(id))
			.forEach(id -> localizedContentRepository.findFirstByPlaceIdAndVisibleTrue(id)
				.ifPresent(c -> nameMap.put(id, c.getName())));

		return nameMap;
	}

	private String buildUserPrompt(
		final List<ScoredCandidate> scored,
		final Map<Long, String> nameByPlaceId,
		final TravelProfile profile,
		final LocalDate tripStart,
		final LocalDate tripEnd
	) {
		StringBuilder sb = new StringBuilder();

		sb.append("[사용자 여행 조건]\n");
		sb.append("- 여행 기간: ").append(tripStart).append(" ~ ").append(tripEnd)
			.append(" (").append(profile.tripDays()).append("일)\n");
		sb.append("- 활동 시간대: ").append(profile.activityStartTime())
			.append(" ~ ").append(profile.activityEndTime()).append("\n");
		sb.append("- 여행 레벨: ").append(profile.experienceLevel().getDisplayName()).append("\n");
		sb.append("- 이동수단: ").append(profile.transportMode().getDisplayName()).append("\n\n");

		sb.append("[후보 장소 목록]\n");
		for (ScoredCandidate s : scored) {
			RecommendationCandidate c = s.candidate();
			String name = nameByPlaceId.getOrDefault(c.placeId(), "장소 " + c.placeId());
			sb.append(String.format(
				"- placeId=%d | %s | 유형=%s | 예상체류=%d분 | 운영=%s | 점수=%.1f\n",
				c.placeId(), name,
				c.contentType().getDisplayName(),
				c.estimatedStayMinutes(),
				formatOperatingHint(c, profile.activityStartTime(), profile.activityEndTime()),
				s.finalScore()
			));
		}

		sb.append("\n위 조건과 후보 목록을 바탕으로 ").append(profile.tripDays())
			.append("일 코스를 JSON으로만 출력하라.");

		return sb.toString();
	}

	/** 운영시간 힌트 문자열. LLM이 시간 배치를 판단하는 데 활용 */
	private String formatOperatingHint(
		final RecommendationCandidate c,
		final LocalTime activityStart,
		final LocalTime activityEnd
	) {
		if (c.weeklySchedule() == null) {
			return "시간미상";
		}
		if (c.isFestival() && c.festivalStartDate() != null) {
			return c.festivalStartDate() + "~" + c.festivalEndDate();
		}
		return activityStart + "~" + activityEnd + " 내 운영";
	}

	/** LLM 응답에서 whitelist 밖 placeId를 가진 방문지를 제거한다 */
	private CourseDraft validateWhitelist(final CourseDraft draft, final Set<Long> whitelist) {
		if (draft.days() == null) {
			return draft;
		}
		List<CourseDraft.DayPlan> cleanedDays = draft.days().stream()
			.map(day -> {
				List<CourseDraft.PlannedVisit> cleanedVisits = day.visits() == null
					? List.of()
					: day.visits().stream()
						.filter(v -> whitelist.contains(v.placeId()))
						.collect(Collectors.toList());
				return new CourseDraft.DayPlan(day.dayNumber(), cleanedVisits);
			})
			.collect(Collectors.toCollection(ArrayList::new));
		return new CourseDraft(cleanedDays);
	}
}
