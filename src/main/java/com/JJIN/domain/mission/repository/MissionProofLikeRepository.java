package com.JJIN.domain.mission.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.mission.entity.MissionProofLike;

public interface MissionProofLikeRepository extends JpaRepository<MissionProofLike, Long> {

	Optional<MissionProofLike> findByMissionProof_IdAndMember_Id(Long missionProofId, Long memberId);

	/**
	 * 주어진 인증글들 중 현재 사용자가 좋아요한 인증글 ID 목록을 조회한다.
	 */
	@Query("""
		select l.missionProof.id from MissionProofLike l
		where l.member.id = :memberId and l.missionProof.id in :proofIds
		""")
	List<Long> findLikedProofIds(
		@Param("memberId") Long memberId,
		@Param("proofIds") Collection<Long> proofIds
	);
}
