package io.github.abcshc.wellnessactivity.activity.api;

import io.github.abcshc.wellnessactivity.activity.service.ActivitySummaryQueryService;
import io.github.abcshc.wellnessactivity.activity.service.DailyActivitySummary;
import io.github.abcshc.wellnessactivity.activity.service.MonthlyActivitySummary;
import io.github.abcshc.wellnessactivity.auth.security.CurrentMemberId;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities/steps")
public class ActivitySummaryController {

	private final ActivitySummaryQueryService activitySummaryQueryService;

	public ActivitySummaryController(ActivitySummaryQueryService activitySummaryQueryService) {
		this.activitySummaryQueryService = activitySummaryQueryService;
	}

	@GetMapping("/daily")
	public ResponseEntity<List<DailyActivitySummaryResponse>> daily(
		@CurrentMemberId Long memberId,
		@RequestParam(required = false) String recordkey,
		@RequestParam(required = false) String from,
		@RequestParam(required = false) String to
	) {
		return ResponseEntity.ok(activitySummaryQueryService.daily(memberId, recordkey, from, to).stream()
			.map(summary -> DailyActivitySummaryResponse.from(recordkey, summary))
			.toList());
	}

	@GetMapping("/monthly")
	public ResponseEntity<List<MonthlyActivitySummaryResponse>> monthly(
		@CurrentMemberId Long memberId,
		@RequestParam(required = false) String recordkey,
		@RequestParam(required = false) String from,
		@RequestParam(required = false) String to
	) {
		return ResponseEntity.ok(activitySummaryQueryService.monthly(memberId, recordkey, from, to).stream()
			.map(summary -> MonthlyActivitySummaryResponse.from(recordkey, summary))
			.toList());
	}

}
