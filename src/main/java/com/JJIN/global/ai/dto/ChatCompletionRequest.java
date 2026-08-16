package com.JJIN.global.ai.dto;

import java.util.List;

public record ChatCompletionRequest(
	String model,
	double temperature,
	List<ChatMessage> messages
) {
}
