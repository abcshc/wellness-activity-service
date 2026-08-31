package io.github.abcshc.wellnessactivity.auth.token.entity;

import io.github.abcshc.wellnessactivity.auth.token.RefreshTokenStatus;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
	name = "refresh_tokens",
	uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = "token_hash"),
	indexes = {
		@Index(name = "idx_refresh_tokens_member_status", columnList = "member_id,status"),
		@Index(name = "idx_refresh_tokens_family_status", columnList = "family_id,status"),
		@Index(name = "idx_refresh_tokens_status_expires_at", columnList = "status,expires_at")
	}
)
public class RefreshTokenEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "member_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_refresh_tokens_member")
	)
	private MemberEntity member;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "family_id", nullable = false, length = 36)
	private String familyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private RefreshTokenStatus status;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "replaced_by_token_id",
		foreignKey = @ForeignKey(name = "fk_refresh_tokens_replaced_by")
	)
	private RefreshTokenEntity replacedByToken;

	protected RefreshTokenEntity() {
	}

	public RefreshTokenEntity(
		MemberEntity member,
		String tokenHash,
		String familyId,
		Instant issuedAt,
		Instant expiresAt
	) {
		this.member = member;
		this.tokenHash = tokenHash;
		this.familyId = familyId;
		this.status = RefreshTokenStatus.ACTIVE;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Long getId() {
		return id;
	}

	public String getFamilyId() {
		return familyId;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public MemberEntity getMember() {
		return member;
	}

	public boolean isActive() {
		return status == RefreshTokenStatus.ACTIVE;
	}

	public boolean isRotated() {
		return status == RefreshTokenStatus.ROTATED;
	}

	public boolean isExpiredAt(Instant now) {
		return !expiresAt.isAfter(now);
	}

	public boolean hasReplacement() {
		return replacedByToken != null;
	}

}
