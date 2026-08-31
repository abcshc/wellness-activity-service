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
	void Samsung과_Apple_시각을_UTC_Instant로_정규화한다() {
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

		assertThat(samsung.command().provider()).isEqualTo(ActivityProvider.SAMSUNG_HEALTH);
		assertThat(apple.command().provider()).isEqualTo(ActivityProvider.APPLE_HEALTH);
		assertThat(samsung.command().records().get(0).startedAtUtc())
			.isEqualTo(Instant.parse("2024-11-15T00:00:00Z"));
		assertThat(apple.command().records().get(0).startedAtUtc())
			.isEqualTo(Instant.parse("2024-11-14T15:00:00Z"));
		assertThat(samsung.invalidEntries()).isEmpty();
		assertThat(apple.invalidEntries()).isEmpty();
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
