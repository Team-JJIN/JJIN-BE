package com.JJIN.global.ai;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.JJIN.domain.mission.exception.MissionErrorCode;
import com.JJIN.global.ai.dto.ChatCompletionRequest;
import com.JJIN.global.ai.dto.ChatCompletionResponse;
import com.JJIN.global.ai.dto.ChatMessage;
import com.JJIN.global.exception.JjinException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RunyourAiChatClient {

	private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
	private static final double TEMPERATURE = 0.0;

	private final RestClient restClient = RestClient.create();

	@Value("${runyour.base-url}")
	private String baseUrl;

	@Value("${runyour.api-key}")
	private String apiKey;

	@Value("${runyour.model}")
	private String model;

	public String chat(final String systemPrompt, final String userPrompt) {
		ChatCompletionRequest request = new ChatCompletionRequest(
			model,
			TEMPERATURE,
			List.of(ChatMessage.system(systemPrompt), ChatMessage.user(userPrompt))
		);

		ChatCompletionResponse response = restClient.post()
			.uri(baseUrl + CHAT_COMPLETIONS_PATH)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.onStatus(HttpStatusCode::isError, (req, res) -> {
				log.error("runyour.ai 호출 실패: status={}", res.getStatusCode());
				throw new JjinException(MissionErrorCode.CATEGORY_CLASSIFICATION_FAILED);
			})
			.body(ChatCompletionResponse.class);

		return response == null ? null : response.firstContent();
	}
}
