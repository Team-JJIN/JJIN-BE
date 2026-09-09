package com.JJIN.domain.mission.entity.converter;

import com.JJIN.domain.mission.entity.enums.UserMissionStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UserMissionStatusConverter implements AttributeConverter<UserMissionStatus, String> {

	private static final String LEGACY_ADDED_STATUS = "ADDED";

	@Override
	public String convertToDatabaseColumn(final UserMissionStatus attribute) {
		return attribute == null ? null : attribute.name();
	}

	@Override
	public UserMissionStatus convertToEntityAttribute(final String dbData) {
		if (dbData == null) {
			return null;
		}
		if (LEGACY_ADDED_STATUS.equals(dbData)) {
			return UserMissionStatus.PROOF_REQUIRED;
		}
		return UserMissionStatus.valueOf(dbData);
	}
}
