package com.JJIN.domain.mission.dto.response;

public record PresignedUrlResponse(
	String presignedUrl,
	String fileName
) {

	public static PresignedUrlResponse of(final String presignedUrl, final String fileName) {
		return new PresignedUrlResponse(presignedUrl, fileName);
	}
}
