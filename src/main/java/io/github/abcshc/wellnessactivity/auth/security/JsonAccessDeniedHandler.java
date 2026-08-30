package io.github.abcshc.wellnessactivity.auth.security;

import io.github.abcshc.wellnessactivity.common.error.CommonErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityErrorResponseWriter errorResponseWriter;

	public JsonAccessDeniedHandler(SecurityErrorResponseWriter errorResponseWriter) {
		this.errorResponseWriter = errorResponseWriter;
	}

	@Override
	public void handle(
		HttpServletRequest request,
		HttpServletResponse response,
		AccessDeniedException accessDeniedException
	) throws IOException, ServletException {
		errorResponseWriter.write(request, response, CommonErrorCode.FORBIDDEN);
	}
}
