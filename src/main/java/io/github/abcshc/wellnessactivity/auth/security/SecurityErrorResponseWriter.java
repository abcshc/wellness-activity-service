package io.github.abcshc.wellnessactivity.auth.security;

import io.github.abcshc.wellnessactivity.common.error.ErrorCode;
import io.github.abcshc.wellnessactivity.common.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorResponseWriter {

	private final ObjectMapper objectMapper;

	public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void write(
		HttpServletRequest request,
		HttpServletResponse response,
		ErrorCode errorCode
	) throws IOException {
		response.setStatus(errorCode.httpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
			Instant.now(),
			errorCode.httpStatus().value(),
			errorCode.code(),
			errorCode.message(),
			request.getRequestURI(),
			List.of()
		));
	}
}
