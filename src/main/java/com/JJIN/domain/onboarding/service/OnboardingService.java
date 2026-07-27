package com.JJIN.domain.onboarding.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JJIN.domain.member.entity.Member;
import com.JJIN.domain.member.exception.MemberErrorCode;
import com.JJIN.domain.member.repository.MemberRepository;
import com.JJIN.domain.onboarding.dto.request.ContentTypePreferenceRequest;
import com.JJIN.domain.onboarding.dto.request.OnboardingCompleteRequest;
import com.JJIN.domain.onboarding.dto.response.OnboardingCompleteResponse;
import com.JJIN.domain.onboarding.dto.response.TravelRegionResponse;
import com.JJIN.domain.onboarding.entity.TravelPlan;
import com.JJIN.domain.onboarding.entity.TravelRegion;
import com.JJIN.domain.onboarding.entity.enums.TravelSubcategory;
import com.JJIN.domain.onboarding.exception.OnboardingErrorCode;
import com.JJIN.domain.onboarding.repository.TravelPlanRepository;
import com.JJIN.domain.onboarding.repository.TravelRegionRepository;
import com.JJIN.domain.onboarding.validator.OnboardingRequestValidator;
import com.JJIN.global.exception.JjinException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

	private final MemberRepository memberRepository;
	private final TravelPlanRepository travelPlanRepository;
	private final TravelRegionRepository travelRegionRepository;
	private final OnboardingRequestValidator onboardingRequestValidator;

	/**
	 * 온보딩에서 수집한 여행 기본 정보를 첫 번째 여행 일정으로 저장한다.
	 * 회원 role 변경 및 토큰 재발급은 /api/auth/role API에서 별도로 처리한다.
	 */
	@Transactional
	public OnboardingCompleteResponse complete(
		final Long memberId,
		final OnboardingCompleteRequest request
	) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new JjinException(MemberErrorCode.MEMBER_NOT_FOUND));

		onboardingRequestValidator.validate(request);

		TravelPlan travelPlan = travelPlanRepository.save(toTravelPlan(member, request));

		log.info("온보딩 여행 일정 생성 완료: memberId={}, travelPlanId={}", memberId, travelPlan.getId());

		return OnboardingCompleteResponse.of(travelPlan.getId());
	}

	@Transactional(readOnly = true)
	public List<TravelRegionResponse> searchRegions(final String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return List.of();
		}
		return travelRegionRepository.findTop20ByDisplayNameContainingOrderByDisplayNameAsc(keyword.trim())
			.stream()
			.map(TravelRegionResponse::from)
			.toList();
	}

	private TravelPlan toTravelPlan(
		final Member member,
		final OnboardingCompleteRequest request
	) {
		TravelRegion region = resolveRegion(request);

		TravelPlan travelPlan = TravelPlan.create(
			member,
			region,
			request.isRegionUndecided(),
			request.startDate(),
			request.endDate(),
			request.activityStartTime(),
			request.activityEndTime(),
			request.transportMode(),
			request.experienceLevel()
		);

		for (ContentTypePreferenceRequest preference : request.preferences()) {
			for (TravelSubcategory subcategory : preference.subcategories()) {
				travelPlan.addPreference(preference.contentType(), subcategory);
			}
		}
		return travelPlan;
	}

	private TravelRegion resolveRegion(final OnboardingCompleteRequest request) {
		if (request.isRegionUndecided()) {
			return null;
		}
		return travelRegionRepository.findById(request.regionId())
			.orElseThrow(() -> new JjinException(OnboardingErrorCode.INVALID_REGION_SELECTION));
	}
}
