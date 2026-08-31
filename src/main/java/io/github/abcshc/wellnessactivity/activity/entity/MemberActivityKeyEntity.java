package io.github.abcshc.wellnessactivity.activity.entity;

import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
	name = "member_activity_keys",
	indexes = {
		@Index(name = "idx_member_activity_keys_member_id", columnList = "member_id"),
		@Index(name = "idx_member_activity_keys_record_key", columnList = "record_key")
	}
)
public class MemberActivityKeyEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "member_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_member_activity_keys_member")
	)
	private MemberEntity member;

	@Column(name = "record_key", nullable = false, length = 255)
	private String recordKey;

	protected MemberActivityKeyEntity() {
	}

	public MemberActivityKeyEntity(MemberEntity member, String recordKey) {
		this.member = member;
		this.recordKey = recordKey;
	}
}
