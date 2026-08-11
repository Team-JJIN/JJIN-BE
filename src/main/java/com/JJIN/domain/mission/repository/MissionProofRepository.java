package com.JJIN.domain.mission.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.JJIN.domain.mission.entity.HotMissionSnapshot;
import com.JJIN.domain.mission.entity.MissionProof;

public interface MissionProofRepository extends JpaRepository<MissionProof, Long> {

	/**
	 * 최신순 피드. mission/member를 fetch join 하여 N+1을 방지한다.
	 */
	@Query("""
		select mp from MissionProof mp
		join fetch mp.mission
		join fetch mp.member
		order by mp.createdAt desc, mp.id desc
		""")
	Slice<MissionProof> findLatestFeed(Pageable pageable);

	/**
	 * 인기순 피드. 좋아요 많은 순으로 정렬한다.
	 */
	@Query("""
		select mp from MissionProof mp
		join fetch mp.mission
		join fetch mp.member
		order by mp.likeCount desc, mp.id desc
		""")
	Slice<MissionProof> findPopularFeed(Pageable pageable);

	/**
	 * 이번 주 핫 피드. '요즘 핫한 미션' 스냅샷(HotMission 집계)에 선정된 미션의 인증글을,
	 * 스냅샷 랭킹(추가수 내림차순, 동점 시 미션 ID 오름차순) 우선으로 노출한다.
	 */
	@Query("""
		select mp from MissionProof mp
		join fetch mp.mission m
		join fetch mp.member
		join HotMissionItem hi on hi.mission = m and hi.snapshot = :snapshot
		order by hi.addedCount desc, m.id asc, mp.createdAt desc, mp.id desc
		""")
	Slice<MissionProof> findWeeklyHotFeed(
		@Param("snapshot") HotMissionSnapshot snapshot,
		Pageable pageable
	);

	/**
	 * 완료 미션 피드. 현재 로그인 사용자가 완료(UserMission.status=COMPLETED)한 미션의 인증글만 최신순으로 조회한다.
	 */
	@Query("""
		select mp from MissionProof mp
		join fetch mp.mission m
		join fetch mp.member
		where m.id in (
			select um.mission.id from UserMission um
			where um.member.id = :memberId
			  and um.status = com.JJIN.domain.mission.entity.enums.UserMissionStatus.COMPLETED
		)
		order by mp.createdAt desc, mp.id desc
		""")
	Slice<MissionProof> findCompletedFeed(
		@Param("memberId") Long memberId,
		Pageable pageable
	);
}
