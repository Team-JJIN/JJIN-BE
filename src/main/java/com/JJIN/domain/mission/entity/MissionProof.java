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
	name = "mission_proof",
	indexes = {
		@Index(name = "idx_mission_proof_mission_id", columnList = "mission_id"),
		@Index(name = "idx_mission_proof_member_id", columnList = "member_id")
	}
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MissionProof extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id", nullable = false)
	private Mission mission;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false, length = 500)
	private String content;

	@Column(name = "image_url", nullable = false, length = 2048)
	private String imageUrl;

	@Builder.Default
	@Column(nullable = false)
	private int likeCount = 0;

	@Builder.Default
	@Column(nullable = false)
	private int commentCount = 0;

	public static MissionProof of(
		final Mission mission,
		final Member member,
		final String content,
		final String imageUrl
	) {
		return MissionProof.builder()
			.mission(mission)
			.member(member)
			.content(content)
			.imageUrl(imageUrl)
			.build();
	}

	public void increaseLikeCount() {
		this.likeCount++;
	}

	public void decreaseLikeCount() {
		if (this.likeCount > 0) {
			this.likeCount--;
		}
	}

	public void increaseCommentCount() {
		this.commentCount++;
	}
}
