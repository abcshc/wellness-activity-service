package io.github.abcshc.wellnessactivity.activity.api;

import io.github.abcshc.wellnessactivity.activity.service.ActivityInputNormalizationResult;
import io.github.abcshc.wellnessactivity.activity.service.ActivityInputNormalizer;
import io.github.abcshc.wellnessactivity.activity.service.ActivityUploadResult;
import io.github.abcshc.wellnessactivity.activity.service.ActivityUploadService;
import io.github.abcshc.wellnessactivity.auth.security.CurrentMemberId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityUploadController {

	private final ActivityInputNormalizer activityInputNormalizer;
	private final ActivityUploadService activityUploadService;

	public ActivityUploadController(
		ActivityInputNormalizer activityInputNormalizer,
		ActivityUploadService activityUploadService
	) {
		this.activityInputNormalizer = activityInputNormalizer;
		this.activityUploadService = activityUploadService;
	}

	@PostMapping("/steps")
	public ResponseEntity<ActivityUploadResponse> uploadSteps(
		@CurrentMemberId Long memberId,
		@RequestBody ActivityUploadRequest request
	) {
		// 항목 단위 검증 결과를 응답에 포함해야 하므로 Bean Validation 대신 정규화 단계에서 검증한다.
		ActivityInputNormalizationResult normalizationResult = activityInputNormalizer.normalize(request);
		ActivityUploadResult result = activityUploadService.upload(memberId, normalizationResult);
		return ResponseEntity.ok(ActivityUploadResponse.from(result));
	}
}
