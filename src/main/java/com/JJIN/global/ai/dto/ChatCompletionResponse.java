package com.JJIN.global.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(
	List<Choice> choices
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Choice(
		ChatMessage message
	) {
	}

	public String firstContent() {
		if (choices == null || choices.isEmpty() || choices.get(0).message() == null) {
			return null;
		}
		return choices.get(0).message().content();
	}
}
