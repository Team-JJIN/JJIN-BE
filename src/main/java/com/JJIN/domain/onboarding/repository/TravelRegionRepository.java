package com.JJIN.domain.onboarding.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.JJIN.domain.onboarding.entity.TravelRegion;

public interface TravelRegionRepository extends JpaRepository<TravelRegion, Long> {

	List<TravelRegion> findTop20ByDisplayNameContainingOrderByDisplayNameAsc(String keyword);
}
