package io.github.abcshc.wellnessactivity.auth.security;

import io.github.abcshc.wellnessactivity.common.error.CommonErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityErrorResponseWriter errorResponseWriter;

	public JsonAuthenticationEntryPoint(SecurityErrorResponseWriter errorResponseWriter) {
		this.errorResponseWriter = errorResponseWriter;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authenticationException
	) throws IOException, ServletException {
		errorResponseWriter.write(request, response, CommonErrorCode.UNAUTHENTICATED);
	}
}
