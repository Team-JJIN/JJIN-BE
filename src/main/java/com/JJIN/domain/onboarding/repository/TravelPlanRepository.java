package com.JJIN.domain.onboarding.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.JJIN.domain.onboarding.entity.TravelPlan;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

	@EntityGraph(attributePaths = "preferences")
	List<TravelPlan> findByMemberIdOrderByCreatedAtDesc(Long memberId);

	Optional<TravelPlan> findByIdAndMemberId(Long id, Long memberId);
}
