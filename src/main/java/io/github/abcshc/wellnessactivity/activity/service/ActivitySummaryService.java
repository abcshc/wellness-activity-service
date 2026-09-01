package io.github.abcshc.wellnessactivity.activity.service;

import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.entity.StepRecordEntity;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ActivitySummaryService {

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
	private static final int ALLOCATION_SCALE = 20;
	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private final StepRecordRepository stepRecordRepository;

	public ActivitySummaryService(StepRecordRepository stepRecordRepository) {
		this.stepRecordRepository = stepRecordRepository;
	}

	public List<DailyActivitySummary> summarizeDaily(
		MemberActivityKeyEntity memberActivityKey,
		LocalDate from,
		LocalDate to
	) {
		if (from.isAfter(to)) {
			throw new IllegalArgumentException("from은 to보다 늦을 수 없습니다.");
		}
		List<TimeBucket<LocalDate>> buckets = dailyBuckets(from, to);
		return aggregate(memberActivityKey, buckets).stream()
			.map(summary -> new DailyActivitySummary(
				summary.label(), summary.total().steps(), summary.total().distanceKm(), summary.total().caloriesKcal()
			))
			.toList();
	}

	public List<MonthlyActivitySummary> summarizeMonthly(
		MemberActivityKeyEntity memberActivityKey,
		YearMonth from,
		YearMonth to
	) {
		if (from.isAfter(to)) {
			throw new IllegalArgumentException("from은 to보다 늦을 수 없습니다.");
		}
		List<TimeBucket<YearMonth>> buckets = monthlyBuckets(from, to);
		return aggregate(memberActivityKey, buckets).stream()
			.map(summary -> new MonthlyActivitySummary(
				summary.label(), summary.total().steps(), summary.total().distanceKm(), summary.total().caloriesKcal()
			))
			.toList();
	}

	private <T> List<LabeledActivityTotal<T>> aggregate(
		MemberActivityKeyEntity memberActivityKey,
		List<TimeBucket<T>> buckets
	) {
		Instant startInclusive = buckets.get(0).startInclusive();
		Instant endExclusive = buckets.get(buckets.size() - 1).endExclusive();
		Map<T, MutableActivityTotal> totals = new LinkedHashMap<>();
		for (TimeBucket<T> bucket : buckets) {
			totals.put(bucket.label(), new MutableActivityTotal());
		}

		for (StepRecordEntity record : stepRecordRepository.findOverlapping(memberActivityKey, startInclusive, endExclusive)) {
			allocate(record, buckets, totals);
		}

		return buckets.stream()
			.map(bucket -> new LabeledActivityTotal<>(bucket.label(), totals.get(bucket.label()).toTotal()))
			.toList();
	}

	private <T> void allocate(
		StepRecordEntity record,
		List<TimeBucket<T>> buckets,
		Map<T, MutableActivityTotal> totals
	) {
		Instant startedAt = record.getStartedAtUtc();
		Instant endedAt = record.getEndedAtUtc();
		if (startedAt.equals(endedAt)) {
			buckets.stream()
				.filter(bucket -> bucket.contains(startedAt))
				.findFirst()
				.ifPresent(bucket -> totals.get(bucket.label()).add(record.getSteps(), record.getDistance(), record.getCalories()));
			return;
		}

		List<TimeBucket<T>> overlaps = buckets.stream().filter(bucket -> bucket.overlaps(startedAt, endedAt)).toList();
		if (overlaps.isEmpty()) {
			return;
		}

		BigDecimal totalDuration = BigDecimal.valueOf(Duration.between(startedAt, endedAt).toNanos());
		BigDecimal remainingSteps = record.getSteps();
		BigDecimal remainingDistance = record.getDistance();
		BigDecimal remainingCalories = record.getCalories();
		boolean coversWholeRecord = !overlaps.get(0).startInclusive().isAfter(startedAt)
			&& !overlaps.get(overlaps.size() - 1).endExclusive().isBefore(endedAt);

		for (int index = 0; index < overlaps.size(); index++) {
			TimeBucket<T> bucket = overlaps.get(index);
			boolean isLastOverlap = index == overlaps.size() - 1;
			BigDecimal steps = coversWholeRecord && isLastOverlap
				? remainingSteps : allocateValue(record.getSteps(), startedAt, endedAt, bucket, totalDuration);
			BigDecimal distance = coversWholeRecord && isLastOverlap
				? remainingDistance : allocateValue(record.getDistance(), startedAt, endedAt, bucket, totalDuration);
			BigDecimal calories = coversWholeRecord && isLastOverlap
				? remainingCalories : allocateValue(record.getCalories(), startedAt, endedAt, bucket, totalDuration);

			totals.get(bucket.label()).add(steps, distance, calories);
			remainingSteps = remainingSteps.subtract(steps);
			remainingDistance = remainingDistance.subtract(distance);
			remainingCalories = remainingCalories.subtract(calories);
		}
	}

	private BigDecimal allocateValue(
		BigDecimal value,
		Instant startedAt,
		Instant endedAt,
		TimeBucket<?> bucket,
		BigDecimal totalDuration
	) {
		Instant overlapStart = startedAt.isAfter(bucket.startInclusive()) ? startedAt : bucket.startInclusive();
		Instant overlapEnd = endedAt.isBefore(bucket.endExclusive()) ? endedAt : bucket.endExclusive();
		BigDecimal overlapDuration = BigDecimal.valueOf(Duration.between(overlapStart, overlapEnd).toNanos());
		return value.multiply(overlapDuration).divide(totalDuration, ALLOCATION_SCALE, RoundingMode.HALF_UP);
	}

	private List<TimeBucket<LocalDate>> dailyBuckets(LocalDate from, LocalDate to) {
		List<TimeBucket<LocalDate>> buckets = new ArrayList<>();
		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			buckets.add(new TimeBucket<>(
				date,
				date.atStartOfDay(BUSINESS_ZONE).toInstant(),
				date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant()
			));
		}
		return buckets;
	}

	private List<TimeBucket<YearMonth>> monthlyBuckets(YearMonth from, YearMonth to) {
		List<TimeBucket<YearMonth>> buckets = new ArrayList<>();
		for (YearMonth month = from; !month.isAfter(to); month = month.plusMonths(1)) {
			buckets.add(new TimeBucket<>(
				month,
				month.atDay(1).atStartOfDay(BUSINESS_ZONE).toInstant(),
				month.plusMonths(1).atDay(1).atStartOfDay(BUSINESS_ZONE).toInstant()
			));
		}
		return buckets;
	}

	private record TimeBucket<T>(T label, Instant startInclusive, Instant endExclusive) {

		private boolean contains(Instant instant) {
			return !instant.isBefore(startInclusive) && instant.isBefore(endExclusive);
		}

		private boolean overlaps(Instant startedAt, Instant endedAt) {
			return startedAt.isBefore(endExclusive) && endedAt.isAfter(startInclusive);
		}
	}

	private record LabeledActivityTotal<T>(T label, ActivityTotal total) {
	}

	private record ActivityTotal(BigDecimal steps, BigDecimal distanceKm, BigDecimal caloriesKcal) {
	}

	private static class MutableActivityTotal {

		private BigDecimal steps = ZERO;
		private BigDecimal distanceKm = ZERO;
		private BigDecimal caloriesKcal = ZERO;

		private void add(BigDecimal steps, BigDecimal distanceKm, BigDecimal caloriesKcal) {
			this.steps = this.steps.add(steps);
			this.distanceKm = this.distanceKm.add(distanceKm);
			this.caloriesKcal = this.caloriesKcal.add(caloriesKcal);
		}

		private ActivityTotal toTotal() {
			return new ActivityTotal(
				normalize(steps),
				normalize(distanceKm),
				normalize(caloriesKcal)
			);
		}

		private BigDecimal normalize(BigDecimal value) {
			BigDecimal normalized = value.stripTrailingZeros();
			return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
		}
	}
}
