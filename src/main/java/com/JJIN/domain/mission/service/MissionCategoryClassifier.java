package com.JJIN.domain.mission.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.global.ai.RunyourAiChatClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자가 입력한 미션 제목/설명/태그를 보고 LLM(runyour.ai)으로
 * TourApiContentType 카테고리 하나를 분류한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionCategoryClassifier {

	private static final TourApiContentType FALLBACK_CATEGORY = TourApiContentType.TOURIST_ATTRACTION;

	private static final String SYSTEM_PROMPT = """
		너는 여행 미션의 카테고리를 분류하는 분류기다.
		사용자가 만들려는 미션의 제목, 설명, 태그를 보고 아래 카테고리 목록 중 가장 적합한 것 하나를 고른다.

		[카테고리 목록 - 영문 코드(한글 설명)]
		%s

		규칙:
		- 반드시 위 목록에 있는 영문 코드 중 정확히 하나만 출력한다.
		- 코드 외의 다른 텍스트, 설명, 문장 부호, 따옴표를 절대 출력하지 않는다.
		- 애매하면 가장 근접한 하나를 고른다.
		""".formatted(buildCategoryCatalog());

	private final RunyourAiChatClient runyourAiChatClient;

	public TourApiContentType classify(final String title, final String description, final List<String> tags) {
		String tagText = (tags == null || tags.isEmpty()) ? "(없음)" : String.join(", ", tags);
		String userPrompt = """
			제목: %s
			설명: %s
			태그: %s
			이 미션에 가장 적합한 카테고리 코드 하나를 출력하라.
			""".formatted(title, description, tagText);

		try {
			String answer = runyourAiChatClient.chat(SYSTEM_PROMPT, userPrompt);
			return parse(answer);
		} catch (RuntimeException e) {
			log.error("미션 카테고리 분류 중 오류 발생. fallback={}", FALLBACK_CATEGORY, e);
			return FALLBACK_CATEGORY;
		}
	}

	private TourApiContentType parse(final String answer) {
		if (answer == null || answer.isBlank()) {
			log.warn("LLM 카테고리 응답이 비어있음. fallback={}", FALLBACK_CATEGORY);
			return FALLBACK_CATEGORY;
		}

		String normalized = answer.trim().toUpperCase();
		return Arrays.stream(TourApiContentType.values())
			.filter(type -> normalized.contains(type.name()))
			.findFirst()
			.orElseGet(() -> {
				log.warn("LLM 카테고리 응답을 해석할 수 없음: '{}'. fallback={}", answer, FALLBACK_CATEGORY);
				return FALLBACK_CATEGORY;
			});
	}

	private static String buildCategoryCatalog() {
		return Arrays.stream(TourApiContentType.values())
			.map(type -> "- %s(%s)".formatted(type.name(), type.getDisplayName()))
			.collect(Collectors.joining("\n"));
	}
}
