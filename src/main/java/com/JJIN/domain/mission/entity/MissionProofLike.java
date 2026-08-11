package com.JJIN.domain.mission.entity;

import com.JJIN.domain.member.entity.Member;
import com.JJIN.global.common.BaseTimeEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "mission_proof_like",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_mission_proof_like_proof_member",
			columnNames = {"mission_proof_id", "member_id"}
		)
	},
	indexes = {
		@Index(name = "idx_mission_proof_like_proof_id", columnList = "mission_proof_id"),
		@Index(name = "idx_mission_proof_like_member_id", columnList = "member_id")
	}
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MissionProofLike extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_proof_id", nullable = false)
	private MissionProof missionProof;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	public static MissionProofLike of(
		final MissionProof missionProof,
		final Member member
	) {
		return MissionProofLike.builder()
			.missionProof(missionProof)
			.member(member)
			.build();
	}
}
