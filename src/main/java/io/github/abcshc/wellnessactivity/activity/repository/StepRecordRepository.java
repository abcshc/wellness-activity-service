package io.github.abcshc.wellnessactivity.activity.repository;

import io.github.abcshc.wellnessactivity.activity.entity.StepRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepRecordRepository extends JpaRepository<StepRecordEntity, Long> {
}
