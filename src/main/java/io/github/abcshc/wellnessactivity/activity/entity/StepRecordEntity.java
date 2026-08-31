package io.github.abcshc.wellnessactivity.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
	name = "step_records",
	indexes = {
		@Index(
			name = "idx_step_records_key_provider_period",
			columnList = "member_activity_key_id,provider,started_at_utc,ended_at_utc"
		),
		@Index(
			name = "idx_step_records_key_started_at_utc",
			columnList = "member_activity_key_id,started_at_utc"
		)
	}
)
public class StepRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "member_activity_key_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_step_records_member_activity_key")
	)
	private MemberActivityKeyEntity memberActivityKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ActivityProvider provider;

	@Column(name = "started_at_utc", nullable = false)
	private Instant startedAtUtc;

	@Column(name = "ended_at_utc", nullable = false)
	private Instant endedAtUtc;

	@Column(nullable = false, precision = 30, scale = 20)
	private BigDecimal steps;

	@Column(name = "distance_km", nullable = false, precision = 30, scale = 20)
	private BigDecimal distance;

	@Column(name = "calories_kcal", nullable = false, precision = 30, scale = 20)
	private BigDecimal calories;

	protected StepRecordEntity() {
	}

	public StepRecordEntity(
		MemberActivityKeyEntity memberActivityKey,
		ActivityProvider provider,
		Instant startedAtUtc,
		Instant endedAtUtc,
		BigDecimal steps,
		BigDecimal distance,
		BigDecimal calories
	) {
		this.memberActivityKey = memberActivityKey;
		this.provider = provider;
		this.startedAtUtc = startedAtUtc;
		this.endedAtUtc = endedAtUtc;
		this.steps = steps;
		this.distance = distance;
		this.calories = calories;
	}
}
