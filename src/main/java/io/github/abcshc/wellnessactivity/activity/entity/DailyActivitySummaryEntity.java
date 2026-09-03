package io.github.abcshc.wellnessactivity.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
	name = "daily_activity_summaries",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_daily_activity_summaries_key_date",
		columnNames = {"member_activity_key_id", "activity_date"}
	)
)
public class DailyActivitySummaryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "member_activity_key_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_daily_activity_summaries_member_activity_key")
	)
	private MemberActivityKeyEntity memberActivityKey;

	// UTC 날짜가 아닌 서비스 기준 시간대(Asia/Seoul)의 활동일이다.
	@Column(name = "activity_date", nullable = false)
	private LocalDate activityDate;

	@Column(nullable = false, precision = 30, scale = 20)
	private BigDecimal steps;

	@Column(name = "distance_km", nullable = false, precision = 30, scale = 20)
	private BigDecimal distanceKm;

	@Column(name = "source_calories_kcal", nullable = false, precision = 30, scale = 20)
	private BigDecimal sourceCaloriesKcal;

	@Column(name = "estimated_calories_kcal", nullable = false, precision = 30, scale = 20)
	private BigDecimal estimatedCaloriesKcal;

	protected DailyActivitySummaryEntity() {
	}

	public DailyActivitySummaryEntity(
		MemberActivityKeyEntity memberActivityKey,
		LocalDate activityDate,
		BigDecimal steps,
		BigDecimal distanceKm,
		BigDecimal sourceCaloriesKcal,
		BigDecimal estimatedCaloriesKcal
	) {
		this.memberActivityKey = memberActivityKey;
		this.activityDate = activityDate;
		this.steps = steps;
		this.distanceKm = distanceKm;
		this.sourceCaloriesKcal = sourceCaloriesKcal;
		this.estimatedCaloriesKcal = estimatedCaloriesKcal;
	}
}
