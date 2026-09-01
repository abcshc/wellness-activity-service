package io.github.abcshc.wellnessactivity.activity.api;

import io.github.abcshc.wellnessactivity.activity.service.ActivitySummaryQueryService;
import io.github.abcshc.wellnessactivity.activity.service.DailyActivitySummary;
import io.github.abcshc.wellnessactivity.activity.service.MonthlyActivitySummary;
import io.github.abcshc.wellnessactivity.common.error.CommonErrorCode;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities/steps")
public class ActivitySummaryController {

	private final ActivitySummaryQueryService activitySummaryQueryService;
	private final MemberRepository memberRepository;

	public ActivitySummaryController(
		ActivitySummaryQueryService activitySummaryQueryService,
		MemberRepository memberRepository
	) {
		this.activitySummaryQueryService = activitySummaryQueryService;
		this.memberRepository = memberRepository;
	}

	@GetMapping("/daily")
	public ResponseEntity<List<DailyActivitySummaryResponse>> daily(
		@AuthenticationPrincipal Jwt jwt,
		@RequestParam(required = false) String recordkey,
		@RequestParam(required = false) String from,
		@RequestParam(required = false) String to
	) {
		MemberEntity member = authenticatedMember(jwt);
		return ResponseEntity.ok(activitySummaryQueryService.daily(member, recordkey, from, to).stream()
			.map(summary -> DailyActivitySummaryResponse.from(recordkey, summary))
			.toList());
	}

	@GetMapping("/monthly")
	public ResponseEntity<List<MonthlyActivitySummaryResponse>> monthly(
		@AuthenticationPrincipal Jwt jwt,
		@RequestParam(required = false) String recordkey,
		@RequestParam(required = false) String from,
		@RequestParam(required = false) String to
	) {
		MemberEntity member = authenticatedMember(jwt);
		return ResponseEntity.ok(activitySummaryQueryService.monthly(member, recordkey, from, to).stream()
			.map(summary -> MonthlyActivitySummaryResponse.from(recordkey, summary))
			.toList());
	}

	private MemberEntity authenticatedMember(Jwt jwt) {
		return memberRepository.findById(Long.parseLong(jwt.getSubject()))
			.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));
	}
}
