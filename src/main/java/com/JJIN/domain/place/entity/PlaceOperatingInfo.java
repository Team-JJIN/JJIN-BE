package com.JJIN.domain.place.entity;

import java.time.LocalDateTime;

import com.JJIN.domain.place.entity.enums.OperatingInfoParseStatus;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소별 운영정보 원문과 검증된 주간 운영 일정을 함께 관리한다.
 */
@Entity
@Table(name = "place_operating_info")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceOperatingInfo {

	@Id
	@Column(name = "place_id")
	private Long placeId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@Lob
	@Column(name = "raw_opening_hours_text", columnDefinition = "text")
	private String rawOpeningHoursText;

	@Lob
	@Column(name = "raw_rest_day_text", columnDefinition = "text")
	private String rawRestDayText;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "weekly_schedule_json", columnDefinition = "json")
	private String weeklyScheduleJson;

	@Enumerated(EnumType.STRING)
	@Column(name = "parse_status", nullable = false, length = 20)
	private OperatingInfoParseStatus parseStatus;

	@Column(name = "parser_version", length = 100)
	private String parserVersion;

	@Column(name = "parsed_at")
	private LocalDateTime parsedAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Builder(access = AccessLevel.PRIVATE)
	private PlaceOperatingInfo(
		final Place place,
		final String rawOpeningHoursText,
		final String rawRestDayText,
		final String weeklyScheduleJson,
		final OperatingInfoParseStatus parseStatus,
		final String parserVersion,
		final LocalDateTime parsedAt
	) {
		this.place = place;
		this.rawOpeningHoursText = rawOpeningHoursText;
		this.rawRestDayText = rawRestDayText;
		this.weeklyScheduleJson = weeklyScheduleJson;
		this.parseStatus = parseStatus;
		this.parserVersion = parserVersion;
		this.parsedAt = parsedAt;
	}

	public static PlaceOperatingInfo create(
		final Place place,
		final String rawOpeningHoursText,
		final String rawRestDayText
	) {
		return PlaceOperatingInfo.builder()
			.place(place)
			.rawOpeningHoursText(rawOpeningHoursText)
			.rawRestDayText(rawRestDayText)
			.parseStatus(OperatingInfoParseStatus.NOT_PARSED)
			.build();
	}

	public void updateRawInformation(
		final String rawOpeningHoursText,
		final String rawRestDayText
	) {
		this.rawOpeningHoursText = rawOpeningHoursText;
		this.rawRestDayText = rawRestDayText;
		this.weeklyScheduleJson = null;
		this.parseStatus = OperatingInfoParseStatus.NOT_PARSED;
		this.parserVersion = null;
		this.parsedAt = null;
	}

	public void updateParsedSchedule(
		final String weeklyScheduleJson,
		final OperatingInfoParseStatus parseStatus,
		final String parserVersion,
		final LocalDateTime parsedAt
	) {
		this.weeklyScheduleJson = weeklyScheduleJson;
		this.parseStatus = parseStatus;
		this.parserVersion = parserVersion;
		this.parsedAt = parsedAt;
	}
}
