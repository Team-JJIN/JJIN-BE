package com.JJIN.domain.mission.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.mission.dto.response.TravelPlanMissionAuthenticationResponse;
import com.JJIN.domain.mission.dto.response.TravelPlanMissionFeedUploadResponse;
import com.JJIN.domain.mission.dto.response.TravelPlanMissionItemResponse;
import com.JJIN.domain.mission.dto.response.TravelPlanMissionListResponse;
import com.JJIN.domain.mission.entity.MissionProof;
import com.JJIN.domain.mission.entity.UserMission;
import com.JJIN.domain.mission.entity.enums.UserMissionStatus;
import com.JJIN.domain.mission.exception.MissionErrorCode;
import com.JJIN.domain.mission.repository.MissionProofRepository;
import com.JJIN.domain.mission.repository.MissionTagMappingRepository;
import com.JJIN.domain.mission.repository.UserMissionRepository;
import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.onboarding.repository.TravelPlanRepository;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TravelPlanMissionService {

	private final TravelPlanRepository travelPlanRepository;
	private final UserMissionRepository userMissionRepository;
	private final MissionProofRepository missionProofRepository;
	private final MissionTagMappingRepository missionTagMappingRepository;
	private final Clock clock;

	@Transactional
	public TravelPlanMissionAuthenticationResponse authenticateTravelPlanMission(
		final Long memberId,
		final Long travelPlanId,
		final Long userMissionId,
		final String proofImageUrl
	) {
		getOwnedTravelPlan(memberId, travelPlanId);
		UserMission userMission = userMissionRepository
			.findByIdAndTravelPlanIdForUpdate(userMissionId, travelPlanId)
			.orElseThrow(() -> new JjinException(MissionErrorCode.USER_MISSION_NOT_FOUND));
		userMission.authenticate(proofImageUrl, LocalDateTime.now(clock));
		return TravelPlanMissionAuthenticationResponse.of(userMission);
	}

	/**
	 * 사진 인증이 끝난(UPLOAD_PENDING) 일정 미션을 피드에 업로드한다.
	 * 새 미션 인증글(MissionProof)을 생성하고 일정 미션 상태를 COMPLETED로 바꾼다.
	 */
	@Transactional
	public TravelPlanMissionFeedUploadResponse uploadTravelPlanMissionFeed(
		final Long memberId,
		final Long travelPlanId,
		final Long userMissionId,
		final String content
	) {
		getOwnedTravelPlan(memberId, travelPlanId);
		UserMission userMission = userMissionRepository
			.findByIdAndTravelPlanIdForUpdate(userMissionId, travelPlanId)
			.orElseThrow(() -> new JjinException(MissionErrorCode.USER_MISSION_NOT_FOUND));

		if (userMission.getStatus() != UserMissionStatus.UPLOAD_PENDING
			|| userMission.getProofImageKey() == null) {
			throw new JjinException(MissionErrorCode.USER_MISSION_NOT_UPLOAD_PENDING);
		}

		// 인증 사진 값을 그대로 피드에 저장하고 상태를 완료로 전환한다.
		MissionProof proof = missionProofRepository.save(MissionProof.of(
			userMission.getMission(),
			userMission.getMember(),
			content,
			userMission.getProofImageKey()
		));
		userMission.complete();

		return TravelPlanMissionFeedUploadResponse.of(proof, userMission, proof.getImageUrl());
	}

	@Transactional(readOnly = true)
	public TravelPlanMissionAuthenticationResponse getTravelPlanMissionAuthentication(
		final Long memberId,
		final Long travelPlanId,
		final Long userMissionId
	) {
		getOwnedTravelPlan(memberId, travelPlanId);
		UserMission userMission = userMissionRepository.findByIdAndTravelPlanId(userMissionId, travelPlanId)
			.orElseThrow(() -> new JjinException(MissionErrorCode.USER_MISSION_NOT_FOUND));
		return TravelPlanMissionAuthenticationResponse.of(userMission);
	}

	@Transactional(readOnly = true)
	public TravelPlanMissionListResponse getTravelPlanMissions(
		final Long memberId,
		final Long travelPlanId,
		final UserMissionStatus status
	) {
		TravelPlan travelPlan = getOwnedTravelPlan(memberId, travelPlanId);
		List<UserMission> userMissions = userMissionRepository
			.findAllByTravelPlanIdOrderByAddedAtDescIdDesc(travelPlanId);

		long proofRequiredCount = countByStatus(userMissions, UserMissionStatus.PROOF_REQUIRED);
		long uploadPendingCount = countByStatus(userMissions, UserMissionStatus.UPLOAD_PENDING);
		long completedCount = countByStatus(userMissions, UserMissionStatus.COMPLETED);
		Map<Long, Long> latestProofIdByMissionId = getLatestProofIdByMissionId(memberId, userMissions);
		Map<Long, List<String>> tagsByMissionId = getTagsByMissionId(userMissions);

		List<TravelPlanMissionItemResponse> missions = userMissions.stream()
			.filter(userMission -> status == null || userMission.getStatus() == status)
			.map(userMission -> TravelPlanMissionItemResponse.of(
				userMission,
				tagsByMissionId.getOrDefault(userMission.getMission().getId(), List.of()),
				userMission.getStatus() == UserMissionStatus.COMPLETED
					? latestProofIdByMissionId.get(userMission.getMission().getId())
					: null
			))
			.toList();

		return TravelPlanMissionListResponse.of(
			travelPlan,
			userMissions.size(),
			proofRequiredCount,
			uploadPendingCount,
			completedCount,
			missions
		);
	}

	@Transactional
	public void deleteTravelPlanMission(
		final Long memberId,
		final Long travelPlanId,
		final Long userMissionId
	) {
		getOwnedTravelPlan(memberId, travelPlanId);
		UserMission userMission = userMissionRepository.findByIdAndTravelPlanId(userMissionId, travelPlanId)
			.orElseThrow(() -> new JjinException(MissionErrorCode.USER_MISSION_NOT_FOUND));

		userMissionRepository.delete(userMission);
	}

	private Map<Long, List<String>> getTagsByMissionId(final List<UserMission> userMissions) {
		List<Long> missionIds = userMissions.stream()
			.map(userMission -> userMission.getMission().getId())
			.distinct()
			.toList();

		if (missionIds.isEmpty()) {
			return Map.of();
		}

		return missionTagMappingRepository.findAllByMissionIdInWithTag(missionIds).stream()
			.collect(Collectors.groupingBy(
				mapping -> mapping.getMission().getId(),
				LinkedHashMap::new,
				Collectors.mapping(mapping -> mapping.getTag().getName(), Collectors.toList())
			));
	}

	private Map<Long, Long> getLatestProofIdByMissionId(
		final Long memberId,
		final List<UserMission> userMissions
	) {
		List<Long> completedMissionIds = userMissions.stream()
			.filter(userMission -> userMission.getStatus() == UserMissionStatus.COMPLETED)
			.map(userMission -> userMission.getMission().getId())
			.distinct()
			.toList();

		if (completedMissionIds.isEmpty()) {
			return Map.of();
		}

		return missionProofRepository
			.findAllByMemberIdAndMissionIdInOrderByCreatedAtDescIdDesc(memberId, completedMissionIds)
			.stream()
			.collect(Collectors.toMap(
				proof -> proof.getMission().getId(),
				MissionProof::getId,
				(latestProofId, ignoredProofId) -> latestProofId
			));
	}

	private TravelPlan getOwnedTravelPlan(final Long memberId, final Long travelPlanId) {
		return travelPlanRepository.findByIdAndMemberId(travelPlanId, memberId)
			.orElseThrow(() -> new JjinException(MissionErrorCode.TRAVEL_PLAN_NOT_FOUND));
	}

	private long countByStatus(
		final List<UserMission> userMissions,
		final UserMissionStatus status
	) {
		return userMissions.stream()
			.filter(userMission -> userMission.getStatus() == status)
			.count();
	}
}
