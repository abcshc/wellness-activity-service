package io.github.abcshc.wellnessactivity.auth.security;

import io.github.abcshc.wellnessactivity.common.error.CommonErrorCode;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentMemberIdArgumentResolver implements HandlerMethodArgumentResolver {

	private final MemberRepository memberRepository;

	public CurrentMemberIdArgumentResolver(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentMemberId.class)
			&& parameter.getParameterType().equals(Long.class);
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
			throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
		}

		Long memberId = memberId(jwtAuthentication);
		if (!memberRepository.existsById(memberId)) {
			throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
		}

		return memberId;
	}

	private Long memberId(JwtAuthenticationToken jwtAuthentication) {
		try {
			return Long.parseLong(jwtAuthentication.getToken().getSubject());
		} catch (NumberFormatException exception) {
			// JWT 서명·만료 검증은 Security Filter가 수행하며, 여기서는 서비스가 발급한 회원 식별자 형식만 해석한다.
			throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
		}
	}
}
