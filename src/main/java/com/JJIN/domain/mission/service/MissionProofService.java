package com.JJIN.domain.mission.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.member.entity.Member;
import com.JJIN.domain.member.repository.MemberRepository;
import com.JJIN.domain.mission.dto.response.MissionProofFeedItemResponse;
import com.JJIN.domain.mission.dto.response.MissionProofFeedResponse;
import com.JJIN.domain.mission.dto.response.MissionProofLikeToggleResponse;
import com.JJIN.domain.mission.dto.response.MissionProofMissionSummaryResponse;
import com.JJIN.domain.mission.entity.HotMissionSnapshot;
import com.JJIN.domain.mission.entity.Mission;
import com.JJIN.domain.mission.entity.MissionProof;
import com.JJIN.domain.mission.entity.MissionProofLike;
import com.JJIN.domain.mission.entity.enums.MissionProofFeedTab;
import com.JJIN.domain.mission.exception.MissionErrorCode;
import com.JJIN.domain.mission.repository.HotMissionSnapshotRepository;
import com.JJIN.domain.mission.repository.MissionProofLikeRepository;
import com.JJIN.domain.mission.repository.MissionProofRepository;
import com.JJIN.domain.mission.repository.UserMissionRepository;
import com.JJIN.domain.mission.repository.dto.MissionMetricProjection;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;

/**
 * 미션 인증 피드 조회 및 인증글 좋아요 토글 서비스.
 */
@Service
@RequiredArgsConstructor
public class MissionProofService {

	private static final int DEFAULT_SIZE = 10;
	private static final int MAX_SIZE = 50;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final MissionProofRepository missionProofRepository;
	private final MissionProofLikeRepository missionProofLikeRepository;
	private final UserMissionRepository userMissionRepository;
	private final HotMissionSnapshotRepository hotMissionSnapshotRepository;
	private final MemberRepository memberRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public MissionProofFeedResponse getFeed(
		final Long memberId,
		final MissionProofFeedTab tab,
		final int page,
		final int size
	) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize);

		LocalDateTime weekStart = currentWeekStart();
		LocalDateTime weekEnd = weekStart.plusWeeks(1);

		Slice<MissionProof> slice = fetchSlice(tab, memberId, pageable);
		List<MissionProof> proofs = slice.getContent();

		List<MissionProofFeedItemResponse> items = toItems(proofs, memberId, weekStart, weekEnd);

		return MissionProofFeedResponse.of(tab, safePage, safeSize, slice.hasNext(), items);
	}

	@Transactional
	public MissionProofLikeToggleResponse toggleLike(final Long memberId, final Long proofId) {
		MissionProof proof = missionProofRepository.findById(proofId)
			.orElseThrow(() -> new JjinException(MissionErrorCode.MISSION_PROOF_NOT_FOUND));

		Optional<MissionProofLike> existing =
			missionProofLikeRepository.findByMissionProof_IdAndMember_Id(proofId, memberId);

		boolean liked;
		if (existing.isPresent()) {
			missionProofLikeRepository.delete(existing.get());
			proof.decreaseLikeCount();
			liked = false;
		} else {
			Member memberRef = memberRepository.getReferenceById(memberId);
			missionProofLikeRepository.save(MissionProofLike.of(proof, memberRef));
			proof.increaseLikeCount();
			liked = true;
		}

		return MissionProofLikeToggleResponse.of(proofId, liked, proof.getLikeCount());
	}

	private Slice<MissionProof> fetchSlice(
		final MissionProofFeedTab tab,
		final Long memberId,
		final Pageable pageable
	) {
		return switch (tab) {
			case LATEST -> missionProofRepository.findLatestFeed(pageable);
			case POPULAR -> missionProofRepository.findPopularFeed(pageable);
			case WEEKLY_HOT -> weeklyHotSlice(pageable);
			case COMPLETED -> missionProofRepository.findCompletedFeed(memberId, pageable);
		};
	}

	/**
	 * 이번 주 핫 피드는 '요즘 핫한 미션' 최신 스냅샷을 기준으로 조회한다.
	 * 아직 집계된 스냅샷이 없으면 노출할 핫 미션이 없으므로 빈 결과를 반환한다.
	 */
	private Slice<MissionProof> weeklyHotSlice(final Pageable pageable) {
		return hotMissionSnapshotRepository.findTopByOrderByComputedAtDesc()
			.map(snapshot -> missionProofRepository.findWeeklyHotFeed(snapshot, pageable))
			.orElseGet(() -> new SliceImpl<>(List.of(), pageable, false));
	}

	/**
	 * slice 내 인증글들의 id/missionId를 모아 likedByMe·weeklyCompletedCount를 벌크로 계산한다.
	 */
	private List<MissionProofFeedItemResponse> toItems(
		final List<MissionProof> proofs,
		final Long memberId,
		final LocalDateTime weekStart,
		final LocalDateTime weekEnd
	) {
		if (proofs.isEmpty()) {
			return List.of();
		}

		List<Long> proofIds = proofs.stream().map(MissionProof::getId).toList();
		List<Long> missionIds = proofs.stream()
			.map(proof -> proof.getMission().getId())
			.distinct()
			.toList();

		Set<Long> likedProofIds = Set.copyOf(
			missionProofLikeRepository.findLikedProofIds(memberId, proofIds));
		Map<Long, Long> weeklyCompletedCounts = userMissionRepository
			.countWeeklyCompletedByMissionIds(missionIds, weekStart, weekEnd).stream()
			.collect(Collectors.toMap(
				MissionMetricProjection::getMissionId,
				MissionMetricProjection::getMetric));

		return proofs.stream()
			.map(proof -> {
				Mission mission = proof.getMission();
				MissionProofMissionSummaryResponse missionSummary = MissionProofMissionSummaryResponse.of(
					mission,
					weeklyCompletedCounts.getOrDefault(mission.getId(), 0L)
				);
				return MissionProofFeedItemResponse.of(
					proof,
					likedProofIds.contains(proof.getId()),
					missionSummary
				);
			})
			.toList();
	}

	/**
	 * Asia/Seoul 기준 이번 주 월요일 00:00 (LocalDateTime).
	 */
	private LocalDateTime currentWeekStart() {
		ZonedDateTime nowSeoul = ZonedDateTime.now(clock).withZoneSameInstant(KST);
		return nowSeoul.toLocalDate()
			.with(DayOfWeek.MONDAY)
			.atStartOfDay();
	}
}
