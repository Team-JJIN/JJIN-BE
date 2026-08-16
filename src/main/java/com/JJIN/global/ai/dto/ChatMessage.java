package com.JJIN.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessage(
	String role,
	String content
) {

	public static ChatMessage system(final String content) {
		return new ChatMessage("system", content);
	}

	public static ChatMessage user(final String content) {
		return new ChatMessage("user", content);
	}
}
