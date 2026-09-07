package com.JJIN.domain.recommendation.dto;

/**
 * 스코어링 결과
 */
public record ScoredCandidate(
	RecommendationCandidate candidate,
	double categoryFit,
	double localityFit,
	double timeFit,
	double finalScore
) {
}
