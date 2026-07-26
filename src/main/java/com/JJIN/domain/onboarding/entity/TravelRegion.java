package com.JJIN.domain.onboarding.entity;

import com.JJIN.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 검색 가능한 여행 지역.
 */
@Entity
@Getter
@Table(
	name = "travel_region",
	indexes = {
		@Index(name = "idx_travel_region_display_name", columnList = "display_name")
	},
	uniqueConstraints = @UniqueConstraint(
		name = "uk_travel_region_legal_dong",
		columnNames = {"ldong_regn_cd", "ldong_signgu_cd"}
	)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelRegion extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "ldong_regn_cd", nullable = false, length = 2)
	private String lDongRegnCd;

	@Column(name = "ldong_signgu_cd", length = 3)
	private String lDongSignguCd;

	@Builder(access = AccessLevel.PRIVATE)
	private TravelRegion(
		final String displayName,
		final String lDongRegnCd,
		final String lDongSignguCd
	) {
		this.displayName = displayName;
		this.lDongRegnCd = lDongRegnCd;
		this.lDongSignguCd = lDongSignguCd;
	}

	public static TravelRegion create(
		final String displayName,
		final String lDongRegnCd,
		final String lDongSignguCd
	) {
		return TravelRegion.builder()
			.displayName(displayName)
			.lDongRegnCd(lDongRegnCd)
			.lDongSignguCd(lDongSignguCd)
			.build();
	}
}
