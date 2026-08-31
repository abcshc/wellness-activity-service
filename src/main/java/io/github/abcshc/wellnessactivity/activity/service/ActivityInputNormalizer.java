package io.github.abcshc.wellnessactivity.activity.service;

import io.github.abcshc.wellnessactivity.activity.api.ActivityDataRequest;
import io.github.abcshc.wellnessactivity.activity.api.ActivityEntryRequest;
import io.github.abcshc.wellnessactivity.activity.api.ActivityMeasureRequest;
import io.github.abcshc.wellnessactivity.activity.api.ActivitySourceRequest;
import io.github.abcshc.wellnessactivity.activity.api.ActivityUploadRequest;
import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.error.ActivityErrorCode;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ActivityInputNormalizer {

	private static final int MAX_DECIMAL_PRECISION = 30;
	private static final int MAX_DECIMAL_SCALE = 20;
	private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER =
		DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
	private static final DateTimeFormatter OFFSET_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
		.append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
		.appendOffset("+HHMM", "Z")
		.toFormatter();

	public ActivityInputNormalizationResult normalize(ActivityUploadRequest request) {
		validateRequest(request);
		ActivityDataRequest data = request.data();
		ActivityProvider provider = normalizeProvider(data.source());
		List<NormalizedStepRecordCommand> records = new ArrayList<>();
		List<ActivityEntryValidationError> invalidEntries = new ArrayList<>();

		for (int index = 0; index < data.entries().size(); index++) {
			ActivityEntryRequest entry = data.entries().get(index);
			Optional<ActivityEntryValidationError> validationError = validateEntry(index, entry);
			if (validationError.isPresent()) {
				invalidEntries.add(validationError.get());
				continue;
			}

			records.add(toCommand(entry));
		}

		return new ActivityInputNormalizationResult(
			new ActivityUploadCommand(request.recordkey(), provider, List.copyOf(records)),
			List.copyOf(invalidEntries)
		);
	}

	private void validateRequest(ActivityUploadRequest request) {
		if (request == null || isBlank(request.recordkey())) {
			throw new BusinessException(ActivityErrorCode.INVALID_RECORD_KEY);
		}
		if (request.data() == null || request.data().entries() == null) {
			throw new BusinessException(ActivityErrorCode.INVALID_ENTRIES);
		}
		if (request.data().source() == null || isBlank(request.data().source().name())) {
			throw new BusinessException(ActivityErrorCode.INVALID_SOURCE);
		}
	}

	private ActivityProvider normalizeProvider(ActivitySourceRequest source) {
		return switch (source.name()) {
			case "SamsungHealth" -> ActivityProvider.SAMSUNG_HEALTH;
			case "Health Kit" -> ActivityProvider.APPLE_HEALTH;
			default -> throw new BusinessException(ActivityErrorCode.INVALID_SOURCE);
		};
	}

	private Optional<ActivityEntryValidationError> validateEntry(int index, ActivityEntryRequest entry) {
		if (entry == null || entry.period() == null) {
			return Optional.of(error(index, "period", ActivityErrorCode.INVALID_PERIOD));
		}
		Optional<Instant> startedAt = parseInstant(entry.period().from());
		if (startedAt.isEmpty()) {
			return Optional.of(error(index, "period.from", ActivityErrorCode.INVALID_PERIOD));
		}
		Optional<Instant> endedAt = parseInstant(entry.period().to());
		if (endedAt.isEmpty() || endedAt.get().isBefore(startedAt.get())) {
			return Optional.of(error(index, "period.to", ActivityErrorCode.INVALID_PERIOD));
		}
		if (!isValidMeasure(entry.steps())) {
			return Optional.of(error(index, "steps", ActivityErrorCode.INVALID_STEPS));
		}
		Optional<ActivityEntryValidationError> distanceError = validateMeasure(
			index, entry.distance(), "distance", "km", ActivityErrorCode.INVALID_DISTANCE_UNIT, ActivityErrorCode.INVALID_DISTANCE
		);
		if (distanceError.isPresent()) {
			return distanceError;
		}
		return validateMeasure(
			index, entry.calories(), "calories", "kcal", ActivityErrorCode.INVALID_CALORIES_UNIT, ActivityErrorCode.INVALID_CALORIES
		);
	}

	private Optional<ActivityEntryValidationError> validateMeasure(
		int index,
		ActivityMeasureRequest measure,
		String field,
		String expectedUnit,
		ActivityErrorCode invalidUnitCode,
		ActivityErrorCode invalidValueCode
	) {
		if (measure == null || !expectedUnit.equals(measure.unit())) {
			return Optional.of(error(index, field + ".unit", invalidUnitCode));
		}
		if (!isValidMeasure(measure.value())) {
			return Optional.of(error(index, field + ".value", invalidValueCode));
		}
		return Optional.empty();
	}

	private NormalizedStepRecordCommand toCommand(ActivityEntryRequest entry) {
		return new NormalizedStepRecordCommand(
			parseInstant(entry.period().from()).orElseThrow(),
			parseInstant(entry.period().to()).orElseThrow(),
			entry.steps(),
			entry.distance().value(),
			entry.calories().value()
		);
	}

	private Optional<Instant> parseInstant(String value) {
		if (isBlank(value)) {
			return Optional.empty();
		}
		try {
			return Optional.of(OffsetDateTime.parse(value, OFFSET_DATE_TIME_FORMATTER).toInstant());
		} catch (DateTimeParseException ignored) {
			try {
				return Optional.of(LocalDateTime.parse(value, LOCAL_DATE_TIME_FORMATTER).toInstant(ZoneOffset.UTC));
			} catch (DateTimeParseException ignoredAgain) {
				return Optional.empty();
			}
		}
	}

	private boolean isValidMeasure(BigDecimal value) {
		return value != null
			&& value.signum() >= 0
			&& value.precision() <= MAX_DECIMAL_PRECISION
			&& value.scale() <= MAX_DECIMAL_SCALE
			&& value.precision() - value.scale() <= MAX_DECIMAL_PRECISION - MAX_DECIMAL_SCALE;
	}

	private ActivityEntryValidationError error(int index, String field, ActivityErrorCode errorCode) {
		return new ActivityEntryValidationError(index, field, errorCode.code(), errorCode.message());
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
