package com.JJIN.domain.mission.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.member.entity.Member;
import com.JJIN.domain.member.repository.MemberRepository;
import com.JJIN.domain.mission.dto.request.CreateMissionRequest;
import com.JJIN.domain.mission.dto.response.PresignedUrlResponse;
import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.MissionTag;
import com.JJIN.domain.mission.entity.MissionTagMapping;
import com.JJIN.domain.mission.entity.enums.MissionSourceType;
import com.JJIN.domain.mission.exception.MissionV2ErrorCode;
import com.JJIN.domain.mission.repository.MissionTagMappingV2Repository;
import com.JJIN.domain.mission.repository.MissionTagV2Repository;
import com.JJIN.domain.mission.repository.MissionV2Repository;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.global.exception.JjinException;
import com.JJIN.global.s3.S3PresignedUrlService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionV2Service {

	private static final String MISSION_IMAGE_PREFIX = "mission/";
	private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
		"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
	);

	private final MissionV2Repository missionV2Repository;
	private final MissionTagV2Repository missionTagV2Repository;
	private final MissionTagMappingV2Repository missionTagMappingV2Repository;
	private final MemberRepository memberRepository;
	private final MissionCategoryClassifier missionCategoryClassifier;
	private final S3PresignedUrlService s3PresignedUrlService;

	public PresignedUrlResponse createPresignedUrl(final String fileName, final String contentType) {
		if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			throw new JjinException(MissionV2ErrorCode.UNSUPPORTED_IMAGE_TYPE);
		}

		String key = MISSION_IMAGE_PREFIX + UUID.randomUUID() + "_" + fileName;
		String presignedUrl = s3PresignedUrlService.generatePutPresignedUrl(key, contentType);
		return PresignedUrlResponse.of(presignedUrl, key);
	}

	@Transactional
	public Long createMission(final Long memberId, final CreateMissionRequest request) {
		TourApiContentType category = missionCategoryClassifier.classify(
			request.title(), request.description(), request.tags()
		);

		Member createdBy = memberRepository.getReferenceById(memberId);

		Mission mission = missionV2Repository.save(
			Mission.create(
				request.title(),
				request.description(),
				request.difficulty(),
				category,
				request.imageUrl(),
				createdBy,
				MissionSourceType.USER_CREATED
			)
		);

		saveTags(mission, request.tags());

		log.info("미션 생성 완료: missionId={}, category={}, memberId={}", mission.getId(), category, memberId);
		return mission.getId();
	}

	private void saveTags(final Mission mission, final List<String> tagNames) {
		if (tagNames == null || tagNames.isEmpty()) {
			return;
		}
		for (String tagName : tagNames) {
			MissionTag tag = missionTagV2Repository.findByName(tagName)
				.orElseGet(() -> missionTagV2Repository.save(MissionTag.of(tagName)));
			missionTagMappingV2Repository.save(MissionTagMapping.of(mission, tag));
		}
	}
}
