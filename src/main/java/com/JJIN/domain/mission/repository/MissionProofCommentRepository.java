package com.JJIN.domain.mission.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.mission.entity.MissionProofComment;

public interface MissionProofCommentRepository extends JpaRepository<MissionProofComment, Long> {

	@Query("""
		select comment from MissionProofComment comment
		join fetch comment.member
		where comment.missionProof.id = :proofId
		order by comment.createdAt asc, comment.id asc
		""")
	Slice<MissionProofComment> findAllByProofId(
		@Param("proofId") Long proofId,
		Pageable pageable
	);
}
