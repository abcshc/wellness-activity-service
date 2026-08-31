package io.github.abcshc.wellnessactivity.activity.error;

import io.github.abcshc.wellnessactivity.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ActivityErrorCode implements ErrorCode {

	INVALID_RECORD_KEY("ACTIVITY_INVALID_RECORD_KEY", "recordkey가 올바르지 않습니다."),
	INVALID_SOURCE("ACTIVITY_INVALID_SOURCE", "활동 데이터 원천이 올바르지 않습니다."),
	INVALID_ENTRIES("ACTIVITY_INVALID_ENTRIES", "활동 항목 목록이 올바르지 않습니다."),
	INVALID_PERIOD("ACTIVITY_INVALID_PERIOD", "활동 기간이 올바르지 않습니다."),
	INVALID_STEPS("ACTIVITY_INVALID_STEPS", "걸음 수가 올바르지 않습니다."),
	INVALID_DISTANCE_UNIT("ACTIVITY_INVALID_DISTANCE_UNIT", "거리 단위는 km이어야 합니다."),
	INVALID_DISTANCE("ACTIVITY_INVALID_DISTANCE", "이동 거리가 올바르지 않습니다."),
	INVALID_CALORIES_UNIT("ACTIVITY_INVALID_CALORIES_UNIT", "칼로리 단위는 kcal이어야 합니다."),
	INVALID_CALORIES("ACTIVITY_INVALID_CALORIES", "소모 칼로리가 올바르지 않습니다.");

	private final String code;
	private final String message;

	ActivityErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

	@Override
	public HttpStatus httpStatus() {
		return HttpStatus.BAD_REQUEST;
	}

	@Override
	public String code() {
		return code;
	}

	@Override
	public String message() {
		return message;
	}
}
