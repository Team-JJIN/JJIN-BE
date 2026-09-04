package com.JJIN.domain.travelplan.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.member.entity.Member;
import com.JJIN.domain.member.exception.MemberErrorCode;
import com.JJIN.domain.member.repository.MemberRepository;
import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.onboarding.entity.TravelRegion;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;
import com.JJIN.domain.onboarding.repository.TravelPlanRepository;
import com.JJIN.domain.onboarding.repository.TravelRegionRepository;
import com.JJIN.domain.travelplan.dto.internal.CreateTravelPlanCommand;
import com.JJIN.domain.travelplan.exception.TravelPlanErrorCode;
import com.JJIN.domain.travelplan.validator.TravelPlanRequestValidator;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanService {

	private final MemberRepository memberRepository;
	private final TravelPlanRepository travelPlanRepository;
	private final TravelRegionRepository travelRegionRepository;
	private final TravelPlanRequestValidator travelPlanRequestValidator;

	@Transactional
	public Long create(final Long memberId, final CreateTravelPlanCommand command) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new JjinException(MemberErrorCode.MEMBER_NOT_FOUND));

		travelPlanRequestValidator.validateCreate(command);
		TravelRegion region = resolveRegion(command);
		TravelPlan travelPlan = TravelPlan.create(
			member,
			command.name().trim(),
			region,
			command.isRegionUndecided(),
			command.startDate(),
			command.endDate(),
			command.activityStartTime(),
			command.activityEndTime(),
			command.transportMode(),
			command.experienceLevel()
		);

		for (CreateTravelPlanCommand.Preference preference : command.preferences()) {
			for (TravelSubcategory subcategory : preference.subcategories()) {
				travelPlan.addPreference(preference.contentType(), subcategory);
			}
		}

		TravelPlan savedTravelPlan = travelPlanRepository.save(travelPlan);
		log.info("여행 일정 생성 완료: memberId={}, travelPlanId={}", memberId, savedTravelPlan.getId());
		return savedTravelPlan.getId();
	}

	private TravelRegion resolveRegion(final CreateTravelPlanCommand command) {
		if (command.isRegionUndecided()) {
			return null;
		}
		return travelRegionRepository.findById(command.regionId())
			.orElseThrow(() -> new JjinException(TravelPlanErrorCode.INVALID_REGION_SELECTION));
	}
}
