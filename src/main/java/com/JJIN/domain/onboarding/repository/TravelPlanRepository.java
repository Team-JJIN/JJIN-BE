package com.JJIN.domain.onboarding.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.JJIN.domain.onboarding.entity.TravelPlan;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

	List<TravelPlan> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
