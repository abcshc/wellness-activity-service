package io.github.abcshc.wellnessactivity.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "members",
	uniqueConstraints = @UniqueConstraint(name = "uk_members_email", columnNames = "email")
)
public class MemberEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 30)
	private String nickname;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 60)
	private String passwordHash;

	protected MemberEntity() {
	}

	public MemberEntity(String name, String nickname, String email, String passwordHash) {
		this.name = name;
		this.nickname = nickname;
		this.email = email;
		this.passwordHash = passwordHash;
	}

	public Long getId() {
		return id;
	}

	public String getPasswordHash() {
		return passwordHash;
	}
}
