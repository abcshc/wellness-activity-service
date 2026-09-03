package io.github.abcshc.wellnessactivity.activity.service;

import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.repository.DailyActivitySummaryRepository;
import io.github.abcshc.wellnessactivity.activity.repository.DailyActivitySummaryView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class ActivitySummaryService {

	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private final DailyActivitySummaryRepository dailyActivitySummaryRepository;

	public ActivitySummaryService(DailyActivitySummaryRepository dailyActivitySummaryRepository) {
		this.dailyActivitySummaryRepository = dailyActivitySummaryRepository;
	}

	public List<DailyActivitySummary> summarizeDaily(
		MemberActivityKeyEntity memberActivityKey,
		LocalDate from,
		LocalDate to
	) {
		if (from.isAfter(to)) {
			throw new IllegalArgumentException("from은 to보다 늦을 수 없습니다.");
		}
		Map<LocalDate, MutableActivityTotal> totals = dailyTotals(from, to);
		apply(
			totals,
			dailyActivitySummaryRepository.findSummaries(memberActivityKey, from, to),
			DailyActivitySummaryView::activityDate
		);
		return totals.entrySet().stream()
			.map(entry -> new DailyActivitySummary(
				entry.getKey(), entry.getValue().steps(), entry.getValue().distanceKm(), entry.getValue().caloriesKcal()
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
		Map<YearMonth, MutableActivityTotal> totals = monthlyTotals(from, to);
		apply(
			totals,
			dailyActivitySummaryRepository.findSummaries(memberActivityKey, from.atDay(1), to.atEndOfMonth()),
			view -> YearMonth.from(view.activityDate())
		);
		return totals.entrySet().stream()
			.map(entry -> new MonthlyActivitySummary(
				entry.getKey(), entry.getValue().steps(), entry.getValue().distanceKm(), entry.getValue().caloriesKcal()
			))
			.toList();
	}

	private Map<LocalDate, MutableActivityTotal> dailyTotals(LocalDate from, LocalDate to) {
		Map<LocalDate, MutableActivityTotal> totals = new LinkedHashMap<>();
		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			totals.put(date, new MutableActivityTotal());
		}
		return totals;
	}

	private Map<YearMonth, MutableActivityTotal> monthlyTotals(YearMonth from, YearMonth to) {
		Map<YearMonth, MutableActivityTotal> totals = new LinkedHashMap<>();
		for (YearMonth month = from; !month.isAfter(to); month = month.plusMonths(1)) {
			totals.put(month, new MutableActivityTotal());
		}
		return totals;
	}

	private <T> void apply(
		Map<T, MutableActivityTotal> totals,
		List<DailyActivitySummaryView> summaries,
		Function<DailyActivitySummaryView, T> labelExtractor
	) {
		for (DailyActivitySummaryView summary : summaries) {
			MutableActivityTotal total = totals.get(labelExtractor.apply(summary));
			if (total != null) {
				total.add(summary);
			}
		}
	}

	private static class MutableActivityTotal {

		private BigDecimal steps = ZERO;
		private BigDecimal distanceKm = ZERO;
		private BigDecimal sourceCaloriesKcal = ZERO;
		private BigDecimal estimatedCaloriesKcal = ZERO;

		private void add(DailyActivitySummaryView summary) {
			steps = steps.add(summary.steps());
			distanceKm = distanceKm.add(summary.distanceKm());
			sourceCaloriesKcal = sourceCaloriesKcal.add(summary.sourceCaloriesKcal());
			estimatedCaloriesKcal = estimatedCaloriesKcal.add(summary.estimatedCaloriesKcal());
		}

		private BigDecimal steps() {
			return normalize(steps);
		}

		private BigDecimal distanceKm() {
			return normalize(distanceKm);
		}

		private BigDecimal caloriesKcal() {
			return normalize(sourceCaloriesKcal.add(estimatedCaloriesKcal));
		}

		private BigDecimal normalize(BigDecimal value) {
			BigDecimal normalized = value.stripTrailingZeros();
			return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
		}
	}
}
