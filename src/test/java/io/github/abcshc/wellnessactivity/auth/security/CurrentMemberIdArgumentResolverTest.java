package io.github.abcshc.wellnessactivity.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import io.github.abcshc.wellnessactivity.common.error.CommonErrorCode;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import java.lang.reflect.Method;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

class CurrentMemberIdArgumentResolverTest {

	private final MemberRepository memberRepository = Mockito.mock(MemberRepository.class);
	private final CurrentMemberIdArgumentResolver resolver = new CurrentMemberIdArgumentResolver(memberRepository);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldSupportAnnotatedLongParameter() throws Exception {
		assertThat(resolver.supportsParameter(parameter("currentMemberId"))).isTrue();
		assertThat(resolver.supportsParameter(parameter("unannotatedMemberId"))).isFalse();
		assertThat(resolver.supportsParameter(parameter("annotatedString"))).isFalse();
	}

	@Test
	void shouldReturnMemberIdWhenJwtSubjectBelongsToExistingMember() throws Exception {
		given(memberRepository.existsById(1L)).willReturn(true);
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt("1")));

		Object resolved = resolver.resolveArgument(
			parameter("currentMemberId"),
			new ModelAndViewContainer(),
			new ServletWebRequest(new MockHttpServletRequest()),
			null
		);

		assertThat(resolved).isEqualTo(1L);
	}

	@Test
	void shouldThrowUnauthenticatedWhenMemberDoesNotExist() throws Exception {
		given(memberRepository.existsById(1L)).willReturn(false);
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt("1")));

		assertThatThrownBy(() -> resolver.resolveArgument(
			parameter("currentMemberId"),
			new ModelAndViewContainer(),
			new ServletWebRequest(new MockHttpServletRequest()),
			null
		))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHENTICATED));
	}

	@Test
	void shouldThrowUnauthenticatedWhenRequestHasNoJwtAuthentication() throws Exception {
		assertThatThrownBy(() -> resolver.resolveArgument(
			parameter("currentMemberId"),
			new ModelAndViewContainer(),
			new ServletWebRequest(new MockHttpServletRequest()),
			null
		))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHENTICATED));
	}

	private MethodParameter parameter(String methodName) throws Exception {
		Method method = SampleController.class.getDeclaredMethod(methodName, parameterType(methodName));
		return new MethodParameter(method, 0);
	}

	private Class<?> parameterType(String methodName) {
		return "annotatedString".equals(methodName) ? String.class : Long.class;
	}

	private Jwt jwt(String subject) {
		Instant issuedAt = Instant.parse("2025-01-01T00:00:00Z");
		return Jwt.withTokenValue("token")
			.header("alg", "HS256")
			.subject(subject)
			.issuedAt(issuedAt)
			.expiresAt(issuedAt.plusSeconds(60))
			.build();
	}

	private static class SampleController {

		void currentMemberId(@CurrentMemberId Long memberId) {
		}

		void unannotatedMemberId(Long memberId) {
		}

		void annotatedString(@CurrentMemberId String memberId) {
		}
	}
}
