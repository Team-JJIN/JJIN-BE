package com.JJIN.domain.mission.entity;

import com.JJIN.domain.member.entity.Member;
import com.JJIN.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "mission_proof_comment",
	indexes = {
		@Index(
			name = "idx_mission_proof_comment_proof_created",
			columnList = "mission_proof_id, created_at, id"
		),
		@Index(name = "idx_mission_proof_comment_member_id", columnList = "member_id")
	}
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MissionProofComment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_proof_id", nullable = false)
	private MissionProof missionProof;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false, length = 500)
	private String content;

	public static MissionProofComment of(
		final MissionProof missionProof,
		final Member member,
		final String content
	) {
		return MissionProofComment.builder()
			.missionProof(missionProof)
			.member(member)
			.content(content)
			.build();
	}
}
