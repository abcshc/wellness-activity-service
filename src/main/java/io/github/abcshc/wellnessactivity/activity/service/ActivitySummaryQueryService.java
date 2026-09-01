package io.github.abcshc.wellnessactivity.activity.service;

import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.error.ActivityErrorCode;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ActivitySummaryQueryService {

	private static final long MAX_DAILY_RANGE_DAYS = 366;
	private static final long MAX_MONTHLY_RANGE_MONTHS = 24;

	private final MemberActivityKeyRepository memberActivityKeyRepository;
	private final ActivitySummaryService activitySummaryService;

	public ActivitySummaryQueryService(
		MemberActivityKeyRepository memberActivityKeyRepository,
		ActivitySummaryService activitySummaryService
	) {
		this.memberActivityKeyRepository = memberActivityKeyRepository;
		this.activitySummaryService = activitySummaryService;
	}

	public List<DailyActivitySummary> daily(Long memberId, String recordKey, String from, String to) {
		LocalDate startDate = parseDate(from);
		LocalDate endDate = parseDate(to);
		validateDailyRange(startDate, endDate);
		return activitySummaryService.summarizeDaily(ownedActivityKey(memberId, recordKey), startDate, endDate);
	}

	public List<MonthlyActivitySummary> monthly(Long memberId, String recordKey, String from, String to) {
		YearMonth startMonth = parseYearMonth(from);
		YearMonth endMonth = parseYearMonth(to);
		validateMonthlyRange(startMonth, endMonth);
		return activitySummaryService.summarizeMonthly(ownedActivityKey(memberId, recordKey), startMonth, endMonth);
	}

	private MemberActivityKeyEntity ownedActivityKey(Long memberId, String recordKey) {
		if (recordKey == null || recordKey.isBlank()) {
			throw new BusinessException(ActivityErrorCode.INVALID_RECORD_KEY);
		}
		return memberActivityKeyRepository.findByRecordKey(recordKey)
			.filter(activityKey -> activityKey.isOwnedBy(memberId))
			.orElseThrow(() -> new BusinessException(ActivityErrorCode.RECORD_KEY_FORBIDDEN));
	}

	private LocalDate parseDate(String value) {
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException | NullPointerException exception) {
			throw new BusinessException(ActivityErrorCode.INVALID_DAILY_RANGE);
		}
	}

	private YearMonth parseYearMonth(String value) {
		try {
			return YearMonth.parse(value);
		} catch (DateTimeParseException | NullPointerException exception) {
			throw new BusinessException(ActivityErrorCode.INVALID_MONTHLY_RANGE);
		}
	}

	private void validateDailyRange(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new BusinessException(ActivityErrorCode.INVALID_DAILY_RANGE);
		}
		if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAILY_RANGE_DAYS) {
			throw new BusinessException(ActivityErrorCode.DAILY_RANGE_TOO_LARGE);
		}
	}

	private void validateMonthlyRange(YearMonth from, YearMonth to) {
		if (from.isAfter(to)) {
			throw new BusinessException(ActivityErrorCode.INVALID_MONTHLY_RANGE);
		}
		if (ChronoUnit.MONTHS.between(from, to) + 1 > MAX_MONTHLY_RANGE_MONTHS) {
			throw new BusinessException(ActivityErrorCode.MONTHLY_RANGE_TOO_LARGE);
		}
	}
}
