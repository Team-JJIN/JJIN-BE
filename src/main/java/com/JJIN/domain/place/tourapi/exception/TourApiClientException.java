package com.JJIN.domain.place.tourapi.exception;

import lombok.Getter;

@Getter
public class TourApiClientException extends RuntimeException {

	private final String resultCode;

	public TourApiClientException(final String message) {
		this(null, message, null);
	}

	public TourApiClientException(final String message, final Throwable cause) {
		this(null, message, cause);
	}

	public TourApiClientException(final String resultCode, final String message) {
		this(resultCode, message, null);
	}

	private TourApiClientException(
		final String resultCode,
		final String message,
		final Throwable cause
	) {
		super(message, cause);
		this.resultCode = resultCode;
	}
}
