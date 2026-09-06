package com.JJIN.domain.recommendation.policy;

import java.util.EnumMap;
import java.util.Map;

import com.JJIN.domain.onboarding.entity.enums.ExperienceLevel;

/**
 * 여행 레벨별 목표 로컬도와 스코어링 가중치.
 *
 * @param targetLocality 목표 로컬도 L_target
 * @param categoryWeight w_cat
 * @param localityWeight w_loc
 * @param timeWeight     w_time
 */
public record LocalityPolicy(
	double targetLocality,
	double categoryWeight,
	double localityWeight,
	double timeWeight
) {

	/** 장소 로컬도 → 레벨 구간 경계 */
	public static final double LIGHT_MAX = 0.35;
	public static final double NORMAL_MAX = 0.70;

	private static final Map<ExperienceLevel, LocalityPolicy> POLICIES = new EnumMap<>(ExperienceLevel.class);

	static {
		POLICIES.put(ExperienceLevel.LIGHT, new LocalityPolicy(0.20, 0.40, 0.30, 0.30));
		POLICIES.put(ExperienceLevel.NORMAL, new LocalityPolicy(0.55, 0.35, 0.40, 0.25));
		POLICIES.put(ExperienceLevel.DEEP, new LocalityPolicy(0.85, 0.30, 0.50, 0.20));
	}

	public static LocalityPolicy of(final ExperienceLevel level) {
		return POLICIES.get(level);
	}

	/**
	 * 장소의 로컬도 점수를 레벨 구간으로 분류한다.
	 */
	public static ExperienceLevel classify(final double localityScore) {
		if (localityScore <= LIGHT_MAX) {
			return ExperienceLevel.LIGHT;
		}
		if (localityScore <= NORMAL_MAX) {
			return ExperienceLevel.NORMAL;
		}
		return ExperienceLevel.DEEP;
	}
}
