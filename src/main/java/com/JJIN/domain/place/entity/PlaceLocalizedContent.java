package com.JJIN.domain.place.entity;

import java.time.LocalDateTime;

import com.JJIN.domain.place.entity.enums.PlaceLocale;
import com.JJIN.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 언어별 TourAPI 식별자와 화면 표시 정보를 함께 관리한다.
 */
@Entity
@Table(
	name = "place_localized_content",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_place_localized_locale_external_content",
			columnNames = {"locale", "external_content_id"}
		),
		@UniqueConstraint(
			name = "uk_place_localized_place_locale",
			columnNames = {"place_id", "locale"}
		)
	},
	indexes = {
		@Index(name = "idx_place_localized_external_modified_at", columnList = "external_modified_at"),
		@Index(name = "idx_place_localized_last_seen_at", columnList = "last_seen_at")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceLocalizedContent extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 5)
	private PlaceLocale locale;

	@Column(name = "external_content_id", nullable = false, length = 50)
	private String externalContentId;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(length = 500)
	private String address;

	@Lob
	@Column(columnDefinition = "text")
	private String overview;

	@Column(name = "representative_image_url", length = 2048)
	private String representativeImageUrl;

	@Column(name = "thumbnail_image_url", length = 2048)
	private String thumbnailImageUrl;

	@Column(name = "image_copyright_type_code", length = 20)
	private String imageCopyrightTypeCode;

	@Column(name = "external_modified_at")
	private LocalDateTime externalModifiedAt;

	@Column(nullable = false)
	private boolean visible;

	@Column(name = "last_seen_at", nullable = false)
	private LocalDateTime lastSeenAt;

	@Builder(access = AccessLevel.PRIVATE)
	private PlaceLocalizedContent(
		final Place place,
		final PlaceLocale locale,
		final String externalContentId,
		final String name,
		final String address,
		final String overview,
		final String representativeImageUrl,
		final String thumbnailImageUrl,
		final String imageCopyrightTypeCode,
		final LocalDateTime externalModifiedAt,
		final boolean visible,
		final LocalDateTime lastSeenAt
	) {
		this.place = place;
		this.locale = locale;
		this.externalContentId = externalContentId;
		this.name = name;
		this.address = address;
		this.overview = overview;
		this.representativeImageUrl = representativeImageUrl;
		this.thumbnailImageUrl = thumbnailImageUrl;
		this.imageCopyrightTypeCode = imageCopyrightTypeCode;
		this.externalModifiedAt = externalModifiedAt;
		this.visible = visible;
		this.lastSeenAt = lastSeenAt;
	}

	public static PlaceLocalizedContent create(
		final Place place,
		final PlaceLocale locale,
		final String externalContentId,
		final String name,
		final String address,
		final String overview,
		final String representativeImageUrl,
		final String thumbnailImageUrl,
		final String imageCopyrightTypeCode,
		final LocalDateTime externalModifiedAt,
		final LocalDateTime lastSeenAt
	) {
		return PlaceLocalizedContent.builder()
			.place(place)
			.locale(locale)
			.externalContentId(externalContentId)
			.name(name)
			.address(address)
			.overview(overview)
			.representativeImageUrl(representativeImageUrl)
			.thumbnailImageUrl(thumbnailImageUrl)
			.imageCopyrightTypeCode(imageCopyrightTypeCode)
			.externalModifiedAt(externalModifiedAt)
			.visible(true)
			.lastSeenAt(lastSeenAt)
			.build();
	}

	public void updateContent(
		final String name,
		final String address,
		final String overview,
		final String representativeImageUrl,
		final String thumbnailImageUrl,
		final String imageCopyrightTypeCode,
		final LocalDateTime externalModifiedAt,
		final LocalDateTime lastSeenAt
	) {
		this.name = name;
		this.address = address;
		this.overview = overview;
		this.representativeImageUrl = representativeImageUrl;
		this.thumbnailImageUrl = thumbnailImageUrl;
		this.imageCopyrightTypeCode = imageCopyrightTypeCode;
		this.externalModifiedAt = externalModifiedAt;
		this.lastSeenAt = lastSeenAt;
	}

	public void markSeen(final LocalDateTime seenAt) {
		this.lastSeenAt = seenAt;
	}

	public void changeVisibility(final boolean visible) {
		this.visible = visible;
	}
}
