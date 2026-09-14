package com.JJIN.global.s3;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.JJIN.domain.mission.exception.MissionErrorCode;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3 객체를 업로드하거나 조회할 수 있는 presigned URL을 발급한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

	private static final Duration PRESIGN_DURATION = Duration.ofHours(1);

	private final S3Presigner s3Presigner;

	@Value("${aws.s3.bucket}")
	private String bucket;

	public String generateGetPresignedUrl(final String key) {
		try {
			GetObjectRequest objectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();
			GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(PRESIGN_DURATION)
				.getObjectRequest(objectRequest)
				.build();
			return s3Presigner.presignGetObject(presignRequest).url().toString();
		} catch (RuntimeException e) {
			log.error("조회용 Presigned URL 생성 실패: key={}", key, e);
			throw new JjinException(MissionErrorCode.PRESIGNED_URL_GENERATION_FAILED);
		}
	}

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
