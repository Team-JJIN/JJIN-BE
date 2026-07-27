package com.JJIN.domain.mission.entity;

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
	name = "mission_proof_image",
	indexes = {
		@Index(name = "idx_mission_proof_image_proof_id", columnList = "proof_id")
	}
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MissionProofImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "proof_id", nullable = false)
	private MissionProof proof;

	@Column(nullable = false, length = 2048)
	private String imageUrl;

	public static MissionProofImage of(final MissionProof proof, final String imageUrl) {
		return MissionProofImage.builder()
			.proof(proof)
			.imageUrl(imageUrl)
			.build();
	}
}
