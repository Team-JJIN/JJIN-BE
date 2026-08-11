package com.JJIN.domain.member.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EmailSignupRequest(
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	String email,

	@NotBlank(message = "이름은 필수입니다.")
	String nickname,

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
		message = "비밀번호는 8자 이상이며 영문과 숫자를 모두 포함해야 합니다."
	)
	String password,

	@NotNull(message = "약관 동의 정보는 필수입니다.")
	@Valid
	List<TermsAgreementRequest> termsAgreements
) {
}
