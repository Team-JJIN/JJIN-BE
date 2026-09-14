package com.JJIN.domain.place.candidate;

import java.util.Collections;
import java.util.List;

public record CandidatePool(
	List<PlaceCandidate> candidates
) {
	public CandidatePool {
		candidates = candidates == null ? Collections.emptyList() : List.copyOf(candidates);
	}
}
