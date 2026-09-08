package com.JJIN.domain.place.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.JJIN.domain.place.entity.PlaceOperatingInfo;

public interface PlaceOperatingInfoRepository extends JpaRepository<PlaceOperatingInfo, Long> {

	List<PlaceOperatingInfo> findAllByPlaceIdIn(Collection<Long> placeIds);
}
