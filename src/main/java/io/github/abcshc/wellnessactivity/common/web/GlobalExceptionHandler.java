package io.github.abcshc.wellnessactivity.common.web;

import io.github.abcshc.wellnessactivity.common.error.CommonErrorCode;
import io.github.abcshc.wellnessactivity.common.error.ErrorCode;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(
		BusinessException exception,
		HttpServletRequest request
	) {
		return toResponse(exception.getErrorCode(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
		MethodArgumentNotValidException exception,
		HttpServletRequest request
	) {
		List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
			.map(fieldError -> new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()))
			.toList();

		return toResponse(CommonErrorCode.INVALID_REQUEST, request, fieldErrors);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(
		Exception exception,
		HttpServletRequest request
	) {
		log.error("Unexpected exception occurred", exception);
		return toResponse(CommonErrorCode.INTERNAL_SERVER_ERROR, request);
	}

	private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, HttpServletRequest request) {
		return toResponse(errorCode, request, List.of());
	}

	private ResponseEntity<ErrorResponse> toResponse(
		ErrorCode errorCode,
		HttpServletRequest request,
		List<FieldErrorResponse> fieldErrors
	) {
		ErrorResponse response = new ErrorResponse(
			Instant.now(),
			errorCode.httpStatus().value(),
			errorCode.code(),
			errorCode.message(),
			request.getRequestURI(),
			fieldErrors
		);

		return ResponseEntity.status(errorCode.httpStatus()).body(response);
	}
}
