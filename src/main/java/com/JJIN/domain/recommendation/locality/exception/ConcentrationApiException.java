package com.JJIN.domain.recommendation.locality.exception;

import lombok.Getter;

@Getter
public class ConcentrationApiException extends RuntimeException {

	private final String resultCode;

	public ConcentrationApiException(final String message) {
		this(null, message, null);
	}

	public ConcentrationApiException(final String message, final Throwable cause) {
		this(null, message, cause);
	}

	public ConcentrationApiException(final String resultCode, final String message) {
		this(resultCode, message, null);
	}

	private ConcentrationApiException(
		final String resultCode,
		final String message,
		final Throwable cause
	) {
		super(message, cause);
		this.resultCode = resultCode;
	}
}
