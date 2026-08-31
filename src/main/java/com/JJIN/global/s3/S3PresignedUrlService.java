package com.JJIN.global.s3;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.JJIN.domain.mission.exception.MissionErrorCode;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3에 클라이언트가 직접 PUT 업로드할 수 있는 presigned URL을 발급한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

	private static final Duration PRESIGN_DURATION = Duration.ofHours(1);

	private final S3Presigner s3Presigner;

	@Value("${aws.s3.bucket}")
	private String bucket;

	public String generatePutPresignedUrl(final String key, final String contentType) {
		try {
			PutObjectRequest objectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(contentType)
				.build();

			PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(PRESIGN_DURATION)
				.putObjectRequest(objectRequest)
				.build();

			PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
			return presigned.url().toString();
		} catch (RuntimeException e) {
			log.error("Presigned URL 생성 실패: key={}", key, e);
			throw new JjinException(MissionErrorCode.PRESIGNED_URL_GENERATION_FAILED);
		}
	}
}
