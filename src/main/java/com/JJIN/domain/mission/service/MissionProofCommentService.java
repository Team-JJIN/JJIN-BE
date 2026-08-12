package com.JJIN.domain.mission.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.member.entity.Member;
import com.JJIN.domain.member.repository.MemberRepository;
import com.JJIN.domain.mission.dto.request.MissionProofCommentCreateRequest;
import com.JJIN.domain.mission.dto.response.MissionProofCommentCreateResponse;
import com.JJIN.domain.mission.dto.response.MissionProofCommentListResponse;
import com.JJIN.domain.mission.dto.response.MissionProofCommentResponse;
import com.JJIN.domain.mission.entity.MissionProof;
import com.JJIN.domain.mission.entity.MissionProofComment;
import com.JJIN.domain.mission.exception.MissionErrorCode;
import com.JJIN.domain.mission.repository.MissionProofCommentRepository;
import com.JJIN.domain.mission.repository.MissionProofRepository;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionProofCommentService {

	private static final int MAX_SIZE = 50;
	private static final int MAX_CONTENT_LENGTH = 500;
	private final MissionProofRepository missionProofRepository;
	private final MissionProofCommentRepository missionProofCommentRepository;
	private final MemberRepository memberRepository;

	@Transactional(readOnly = true)
	public MissionProofCommentListResponse getComments(
		final Long proofId,
		final int page,
		final int size
	) {
		if (!missionProofRepository.existsById(proofId)) {
			throw new JjinException(MissionErrorCode.MISSION_PROOF_NOT_FOUND);
		}

		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize);
		Slice<MissionProofComment> commentSlice =
			missionProofCommentRepository.findAllByProofId(proofId, pageable);

		List<MissionProofCommentResponse> comments = commentSlice.getContent().stream()
			.map(MissionProofCommentResponse::from)
			.toList();

		return MissionProofCommentListResponse.of(
			proofId,
			comments,
			safePage,
			safeSize,
			commentSlice.hasNext()
		);
	}

	@Transactional
	public MissionProofCommentCreateResponse createComment(
		final Long memberId,
		final Long proofId,
		final MissionProofCommentCreateRequest request
	) {
		if (request == null) {
			throw new JjinException(MissionErrorCode.MISSION_PROOF_COMMENT_CONTENT_REQUIRED);
		}
		String content = validateAndNormalizeContent(request.content());
		MissionProof proof = missionProofRepository.findByIdForUpdate(proofId)
			.orElseThrow(() -> new JjinException(MissionErrorCode.MISSION_PROOF_NOT_FOUND));
		Member member = memberRepository.getReferenceById(memberId);

		MissionProofComment comment = missionProofCommentRepository.save(
			MissionProofComment.of(proof, member, content)
		);
		proof.increaseCommentCount();

		return MissionProofCommentCreateResponse.of(comment, proof.getCommentCount());
	}

	private String validateAndNormalizeContent(final String content) {
		if (content == null) {
			throw new JjinException(MissionErrorCode.MISSION_PROOF_COMMENT_CONTENT_REQUIRED);
		}

		String normalizedContent = content.trim();
		if (normalizedContent.isEmpty() || normalizedContent.length() > MAX_CONTENT_LENGTH) {
			throw new JjinException(MissionErrorCode.MISSION_PROOF_COMMENT_CONTENT_INVALID);
		}
		return normalizedContent;
	}
}
