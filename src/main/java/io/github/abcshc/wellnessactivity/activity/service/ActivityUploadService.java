package io.github.abcshc.wellnessactivity.activity.service;

import io.github.abcshc.wellnessactivity.activity.entity.MemberActivityKeyEntity;
import io.github.abcshc.wellnessactivity.activity.error.ActivityErrorCode;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityUploadService {

	private final MemberActivityKeyRepository memberActivityKeyRepository;
	private final StepRecordRepository stepRecordRepository;

	public ActivityUploadService(
		MemberActivityKeyRepository memberActivityKeyRepository,
		StepRecordRepository stepRecordRepository
	) {
		this.memberActivityKeyRepository = memberActivityKeyRepository;
		this.stepRecordRepository = stepRecordRepository;
	}

	@Transactional
	public ActivityUploadResult upload(MemberEntity member, ActivityInputNormalizationResult normalizationResult) {
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
		memberActivityKeyRepository.insertIgnore(member.getId(), command.recordKey());
		MemberActivityKeyEntity memberActivityKey = memberActivityKeyRepository
			.findByRecordKey(command.recordKey())
			.orElseThrow();
		if (!memberActivityKey.isOwnedBy(member)) {
			throw new BusinessException(ActivityErrorCode.RECORD_KEY_FORBIDDEN);
		}

		int createdCount = 0;
		for (NormalizedStepRecordCommand record : command.records()) {
			createdCount += stepRecordRepository.insertIgnore(
				memberActivityKey.getId(),
				command.provider().name(),
				record.startedAtUtc(),
				record.endedAtUtc(),
				record.steps(),
				record.distanceKm(),
				record.caloriesKcal()
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
}
