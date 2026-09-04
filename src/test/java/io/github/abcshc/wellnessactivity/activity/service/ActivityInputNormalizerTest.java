package io.github.abcshc.wellnessactivity.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.abcshc.wellnessactivity.activity.api.ActivityDataRequest;
import io.github.abcshc.wellnessactivity.activity.api.ActivityEntryRequest;
import io.github.abcshc.wellnessactivity.activity.api.ActivityMeasureRequest;
import io.github.abcshc.wellnessactivity.activity.api.ActivityPeriodRequest;
import io.github.abcshc.wellnessactivity.activity.api.ActivitySourceRequest;
import io.github.abcshc.wellnessactivity.activity.api.ActivityUploadRequest;
import io.github.abcshc.wellnessactivity.activity.entity.ActivityProvider;
import io.github.abcshc.wellnessactivity.activity.error.ActivityErrorCode;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ActivityInputNormalizerTest {

	private final ActivityInputNormalizer normalizer = new ActivityInputNormalizer();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void 제공_JSON의_숫자와_문자열_걸음수를_같은_DTO로_역직렬화한다() throws Exception {
		String json = """
			{
			  "recordkey": "record-key-001",
			  "data": {
			    "memo": "optional metadata",
			    "entries": [
			      {
			        "period": {"from": "2024-11-15 00:00:00", "to": "2024-11-15 00:10:00"},
			        "steps": 32,
			        "distance": {"unit": "km", "value": 0.02422},
			        "calories": {"unit": "kcal", "value": 1.21}
			      },
			      {
			        "period": {"from": "2024-11-14T15:00:00+0000", "to": "2024-11-14T15:10:00+0000"},
			        "steps": "32.5",
			        "distance": {"unit": "km", "value": 0.02422},
			        "calories": {"unit": "kcal", "value": 1.21}
			      }
			    ],
			    "source": {"name": "SamsungHealth", "mode": 9}
			  }
			  ,"lastUpdate": "2024-11-15 00:10:00 +0000"
			}
			""";

		ActivityUploadRequest request = objectMapper.readValue(json, ActivityUploadRequest.class);

		assertThat(request.data().entries())
			.extracting(ActivityEntryRequest::steps)
			.containsExactly(new BigDecimal("32"), new BigDecimal("32.5"));
	}

	@Test
	void Samsung과_Apple_HealthConnect_시각을_UTC_Instant로_정규화한다() {
		ActivityInputNormalizationResult samsung = normalizer.normalize(upload(
			"SamsungHealth",
			List.of(entry("2024-11-15 00:00:00", "2024-11-15 00:10:00", "32", "0.02422", "1.21")),
			null
		));
		ActivityInputNormalizationResult apple = normalizer.normalize(upload(
			"Health Kit",
			List.of(entry("2024-11-14T15:00:00+0000", "2024-11-14T15:10:00+0000", "32", "0.02422", "1.21")),
			"steps"
		));
		ActivityInputNormalizationResult healthConnect = normalizer.normalize(upload(
			"HealthConnect",
			List.of(entry("2024-11-14T15:00:00+0000", "2024-11-14T15:10:00+0000", "32", "0.02422", "1.21")),
			"steps"
		));

		assertThat(samsung.command().provider()).isEqualTo(ActivityProvider.SAMSUNG_HEALTH);
		assertThat(apple.command().provider()).isEqualTo(ActivityProvider.APPLE_HEALTH);
		assertThat(healthConnect.command().provider()).isEqualTo(ActivityProvider.HEALTH_CONNECT);
		assertThat(samsung.command().records().get(0).startedAtUtc())
			.isEqualTo(Instant.parse("2024-11-15T00:00:00Z"));
		assertThat(apple.command().records().get(0).startedAtUtc())
			.isEqualTo(Instant.parse("2024-11-14T15:00:00Z"));
		assertThat(healthConnect.command().records().get(0).startedAtUtc())
			.isEqualTo(Instant.parse("2024-11-14T15:00:00Z"));
		assertThat(samsung.invalidEntries()).isEmpty();
		assertThat(apple.invalidEntries()).isEmpty();
		assertThat(healthConnect.invalidEntries()).isEmpty();
	}

	@Test
	void 종료_시각이_같은_0초_구간과_소수_측정값을_유효하게_보존한다() {
		ActivityInputNormalizationResult result = normalizer.normalize(upload(
			"SamsungHealth",
			List.of(entry(
				"2024-11-15 00:00:00",
				"2024-11-15 00:00:00",
				"688.5509846105425",
				"0.550840787688434",
				"0"
			)),
			"ignored-type"
		));

		NormalizedStepRecordCommand record = result.command().records().get(0);
		assertThat(record.endedAtUtc()).isEqualTo(record.startedAtUtc());
		assertThat(record.steps()).isEqualByComparingTo("688.5509846105425");
		assertThat(record.distanceKm()).isEqualByComparingTo("0.550840787688434");
		assertThat(record.caloriesKcal()).isZero();
	}

	@Test
	void 항목별_오류는_해당_인덱스와_필드로_분리하고_정상_항목은_유지한다() {
		ActivityInputNormalizationResult result = normalizer.normalize(upload(
			"SamsungHealth",
			List.of(
				entry("2024-11-15 00:10:00", "2024-11-15 00:00:00", "1", "0.1", "1"),
				entry("2024-11-15 00:00:00", "2024-11-15 00:10:00", "-1", "0.1", "1"),
				entry("2024-11-15 00:00:00", "2024-11-15 00:10:00", "1", "0.1", "1", "m"),
				entry("2024-11-15 00:00:00", "2024-11-15 00:10:00", "1", "0.1", "1"))
		));

		assertThat(result.command().records()).hasSize(1);
		assertThat(result.invalidEntries())
			.extracting(ActivityEntryValidationError::index, ActivityEntryValidationError::field,
				ActivityEntryValidationError::code)
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple(0, "period.to", "ACTIVITY_INVALID_PERIOD"),
				org.assertj.core.groups.Tuple.tuple(1, "steps", "ACTIVITY_INVALID_STEPS"),
				org.assertj.core.groups.Tuple.tuple(2, "distance.unit", "ACTIVITY_INVALID_DISTANCE_UNIT")
			);
	}

	@Test
	void 잘못된_일시와_데이터베이스_정밀도_범위를_벗어난_수치는_항목별로_제외한다() {
		ActivityInputNormalizationResult result = normalizer.normalize(upload(
			"SamsungHealth",
			List.of(
				entry(
					"2024-11-15 00:00:00", "2024-11-15 00:10:00",
					"1234567890.12345678901234567890", "0.1", "1"
				),
				entry("invalid-date", "2024-11-15 00:10:00", "1", "0.1", "1"),
				entry("2024-11-15 00:00:00", "2024-11-15 00:10:00", "1", "1.123456789012345678901", "1"),
				entry("2024-11-15 00:00:00", "2024-11-15 00:10:00", "1", "0.1", "12345678901.1234567890123456789")
			)
		));

		assertThat(result.command().records()).singleElement().satisfies(record ->
			assertThat(record.steps()).isEqualByComparingTo("1234567890.12345678901234567890")
		);
		assertThat(result.invalidEntries())
			.extracting(ActivityEntryValidationError::index, ActivityEntryValidationError::field,
				ActivityEntryValidationError::code)
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple(1, "period.from", "ACTIVITY_INVALID_PERIOD"),
				org.assertj.core.groups.Tuple.tuple(2, "distance.value", "ACTIVITY_INVALID_DISTANCE"),
				org.assertj.core.groups.Tuple.tuple(3, "calories.value", "ACTIVITY_INVALID_CALORIES")
			);
	}

	@Test
	void recordkey와_data_source_오류는_전체_요청을_거부한다() {
		assertThatThrownBy(() -> normalizer.normalize(upload("SamsungHealth", List.of(), "steps", "  ")))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ActivityErrorCode.INVALID_RECORD_KEY)
			);
		assertThatThrownBy(() -> normalizer.normalize(upload("Unknown", List.of(), "steps")))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ActivityErrorCode.INVALID_SOURCE)
			);
	}

	@Test
	void 활동_항목이_1000건을_초과하면_전체_요청을_거부한다() {
		ActivityEntryRequest validEntry = entry(
			"2024-11-15 00:00:00", "2024-11-15 00:10:00", "1", "0.1", "1"
		);

		assertThatThrownBy(() -> normalizer.normalize(upload(
			"SamsungHealth", Collections.nCopies(1_001, validEntry), "steps"
		)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ActivityErrorCode.ENTRIES_TOO_MANY)
			);
	}

	private ActivityUploadRequest upload(String sourceName, List<ActivityEntryRequest> entries) {
		return upload(sourceName, entries, "steps");
	}

	private ActivityUploadRequest upload(String sourceName, List<ActivityEntryRequest> entries, String type) {
		return upload(sourceName, entries, type, "record-key-001");
	}

	private ActivityUploadRequest upload(
		String sourceName,
		List<ActivityEntryRequest> entries,
		String type,
		String recordKey
	) {
		return new ActivityUploadRequest(
			recordKey,
			new ActivityDataRequest(entries, new ActivitySourceRequest(sourceName)),
			type
		);
	}

	private ActivityEntryRequest entry(String from, String to, String steps, String distance, String calories) {
		return entry(from, to, steps, distance, calories, "km");
	}

	private ActivityEntryRequest entry(
		String from,
		String to,
		String steps,
		String distance,
		String calories,
		String distanceUnit
	) {
		return new ActivityEntryRequest(
			new ActivityPeriodRequest(from, to),
			new BigDecimal(steps),
			new ActivityMeasureRequest(distanceUnit, new BigDecimal(distance)),
			new ActivityMeasureRequest("kcal", new BigDecimal(calories))
		);
	}
}
