package com.JJIN.domain.place.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;
import com.JJIN.domain.onboarding.entity.enums.TourApiContentType;
import com.JJIN.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 언어와 TourAPI 콘텐츠 ID에 독립적인 물리적 장소.
 */
@Entity
@Table(
	name = "place",
	indexes = {
		@Index(name = "idx_place_content_type", columnList = "content_type"),
		@Index(
			name = "idx_place_legal_dong",
			columnList = "legal_dong_region_code, legal_dong_district_code"
		),
		@Index(
			name = "idx_place_classification",
			columnList = "lcls_systm1_code, lcls_systm2_code"
		),
		@Index(name = "idx_place_coordinates", columnList = "longitude, latitude"),
		@Index(
			name = "idx_place_festival_period",
			columnList = "festival_start_date, festival_end_date"
		)
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "content_type", nullable = false, length = 30)
	private TourApiContentType contentType;

	@Column(nullable = false, precision = 11, scale = 8)
	private BigDecimal longitude;

	@Column(nullable = false, precision = 10, scale = 8)
	private BigDecimal latitude;

	@Column(name = "legal_dong_region_code", length = 20)
	private String legalDongRegionCode;

	@Column(name = "legal_dong_district_code", length = 20)
	private String legalDongDistrictCode;

	@Column(name = "lcls_systm1_code", length = 20)
	private String lclsSystm1Code;

	@Column(name = "lcls_systm2_code", length = 20)
	private String lclsSystm2Code;

	@Column(name = "festival_start_date")
	private LocalDate festivalStartDate;

	@Column(name = "festival_end_date")
	private LocalDate festivalEndDate;

	@Column(name = "festival_time_text", length = 500)
	private String festivalTimeText;

	@Column(name = "festival_place", length = 255)
	private String festivalPlace;

	@Column(name = "locality_score")
	private Double localityScore;

	@Enumerated(EnumType.STRING)
	@Column(name = "locality_level", length = 10)
	private ExperienceLevel localityLevel;

	@Column(name = "locality_version", length = 50)
	private String localityVersion;

	@Column(name = "locality_calculated_at")
	private LocalDateTime localityCalculatedAt;

	@Builder(access = AccessLevel.PRIVATE)
	private Place(
		final TourApiContentType contentType,
		final BigDecimal longitude,
		final BigDecimal latitude,
		final String legalDongRegionCode,
		final String legalDongDistrictCode,
		final String lclsSystm1Code,
		final String lclsSystm2Code
	) {
		this.contentType = contentType;
		this.longitude = longitude;
		this.latitude = latitude;
		this.legalDongRegionCode = legalDongRegionCode;
		this.legalDongDistrictCode = legalDongDistrictCode;
		this.lclsSystm1Code = lclsSystm1Code;
		this.lclsSystm2Code = lclsSystm2Code;
	}

	public static Place create(
		final TourApiContentType contentType,
		final BigDecimal longitude,
		final BigDecimal latitude,
		final String legalDongRegionCode,
		final String legalDongDistrictCode,
		final String lclsSystm1Code,
		final String lclsSystm2Code
	) {
		return Place.builder()
			.contentType(contentType)
			.longitude(longitude)
			.latitude(latitude)
			.legalDongRegionCode(legalDongRegionCode)
			.legalDongDistrictCode(legalDongDistrictCode)
			.lclsSystm1Code(lclsSystm1Code)
			.lclsSystm2Code(lclsSystm2Code)
			.build();
	}

	public void updateBasicInformation(
		final TourApiContentType contentType,
		final BigDecimal longitude,
		final BigDecimal latitude,
		final String legalDongRegionCode,
		final String legalDongDistrictCode,
		final String lclsSystm1Code,
		final String lclsSystm2Code
	) {
		this.contentType = contentType;
		this.longitude = longitude;
		this.latitude = latitude;
		this.legalDongRegionCode = legalDongRegionCode;
		this.legalDongDistrictCode = legalDongDistrictCode;
		this.lclsSystm1Code = lclsSystm1Code;
		this.lclsSystm2Code = lclsSystm2Code;
	}

	public void updateFestivalInformation(
		final LocalDate festivalStartDate,
		final LocalDate festivalEndDate,
		final String festivalTimeText,
		final String festivalPlace
	) {
		this.festivalStartDate = festivalStartDate;
		this.festivalEndDate = festivalEndDate;
		this.festivalTimeText = festivalTimeText;
		this.festivalPlace = festivalPlace;
	}

	public void updateLocality(
		final Double localityScore,
		final ExperienceLevel localityLevel,
		final String localityVersion,
		final LocalDateTime localityCalculatedAt
	) {
		this.localityScore = localityScore;
		this.localityLevel = localityLevel;
		this.localityVersion = localityVersion;
		this.localityCalculatedAt = localityCalculatedAt;
	}
}
