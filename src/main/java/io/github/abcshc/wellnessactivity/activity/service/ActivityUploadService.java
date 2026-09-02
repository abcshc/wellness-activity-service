package io.github.abcshc.wellnessactivity.activity.service;

import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.error.ActivityErrorCode;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordBatchInsert;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordBatchRepository;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityUploadService {

	private static final StepCaloriesEstimator STEP_CALORIES_ESTIMATOR = new StepCaloriesEstimator();
	private static final int STEP_RECORD_BATCH_SIZE = 200;

	private final MemberActivityKeyRepository memberActivityKeyRepository;
	private final StepRecordBatchRepository stepRecordBatchRepository;

	public ActivityUploadService(
		MemberActivityKeyRepository memberActivityKeyRepository,
		StepRecordBatchRepository stepRecordBatchRepository
	) {
		this.memberActivityKeyRepository = memberActivityKeyRepository;
		this.stepRecordBatchRepository = stepRecordBatchRepository;
	}

	@Transactional
	public ActivityUploadResult upload(Long memberId, ActivityInputNormalizationResult normalizationResult) {
		ActivityUploadCommand command = normalizationResult.command();
		if (command.records().isEmpty()) {
			return new ActivityUploadResult(
				normalizationResult.invalidEntries().size(),
				0,
				0,
				normalizationResult.invalidEntries().size(),
				normalizationResult.invalidEntries()
			);
		}
		memberActivityKeyRepository.insertIgnore(memberId, command.recordKey());
		MemberActivityKeyEntity memberActivityKey = memberActivityKeyRepository
			.findByRecordKey(command.recordKey())
			.orElseThrow();
		if (!memberActivityKey.isOwnedBy(memberId)) {
			throw new BusinessException(ActivityErrorCode.RECORD_KEY_FORBIDDEN);
		}

		List<StepRecordBatchInsert> inserts = toBatchInserts(command.records());
		int createdCount = 0;
		for (int startIndex = 0; startIndex < inserts.size(); startIndex += STEP_RECORD_BATCH_SIZE) {
			int endIndex = Math.min(startIndex + STEP_RECORD_BATCH_SIZE, inserts.size());
			createdCount += stepRecordBatchRepository.insertIgnore(
				memberActivityKey.getId(), command.provider(), inserts.subList(startIndex, endIndex)
			);
		}

		return new ActivityUploadResult(
			command.records().size() + normalizationResult.invalidEntries().size(),
			createdCount,
			command.records().size() - createdCount,
			normalizationResult.invalidEntries().size(),
			normalizationResult.invalidEntries()
		);
	}

	private List<StepRecordBatchInsert> toBatchInserts(List<NormalizedStepRecordCommand> records) {
		List<StepRecordBatchInsert> inserts = new ArrayList<>(records.size());
		for (NormalizedStepRecordCommand record : records) {
			StepCaloriesEstimator.StepCaloriesEstimate estimatedCalories = STEP_CALORIES_ESTIMATOR.estimate(
				record.steps(), record.caloriesKcal()
			);
			inserts.add(new StepRecordBatchInsert(
				record.startedAtUtc(),
				record.endedAtUtc(),
				record.steps(),
				record.distanceKm(),
				record.caloriesKcal(),
				estimatedCalories.caloriesKcal(),
				estimatedCalories.version()
			));
		}
		return inserts;
	}
}
