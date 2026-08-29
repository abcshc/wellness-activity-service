package io.github.abcshc.wellnessactivity.member.service;

import io.github.abcshc.wellnessactivity.common.exception.BusinessException;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.error.MemberErrorCode;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class MemberRegistrationService {

	private static final String EMAIL_UNIQUE_CONSTRAINT_NAME = "uk_members_email";

	private final MemberRepository memberRepository;
	private final PasswordHasher passwordHasher;

	public MemberRegistrationService(MemberRepository memberRepository, PasswordHasher passwordHasher) {
		this.memberRepository = memberRepository;
		this.passwordHasher = passwordHasher;
	}

	public MemberRegistrationResult register(MemberRegistrationCommand command) {
		String normalizedEmail = normalizeEmail(command.email());

		if (memberRepository.existsByEmail(normalizedEmail)) {
			throw new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
		}

		String passwordHash = passwordHasher.hash(command.password());
		MemberEntity member = new MemberEntity(
			command.name(),
			command.nickname(),
			normalizedEmail,
			passwordHash
		);

		try {
			memberRepository.saveAndFlush(member);
		} catch (DataIntegrityViolationException exception) {
			if (isEmailUniqueConstraintViolation(exception)) {
				throw new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
			}

			throw exception;
		}

		return new MemberRegistrationResult(command.name(), command.nickname(), normalizedEmail);
	}

	private String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	/**
	 * MySQL/Hibernate가 추가하는 테이블 접두사를 제외하고, 이메일 유니크 제약조건만 중복 오류로 변환한다.
	 */
	private boolean isEmailUniqueConstraintViolation(DataIntegrityViolationException exception) {
		Throwable cause = exception;

		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintViolation
				&& isEmailUniqueConstraintName(constraintViolation.getConstraintName())) {
				return true;
			}
			cause = cause.getCause();
		}

		return false;
	}

	private boolean isEmailUniqueConstraintName(String constraintName) {
		return constraintName != null
			&& (EMAIL_UNIQUE_CONSTRAINT_NAME.equals(constraintName)
			|| constraintName.endsWith("." + EMAIL_UNIQUE_CONSTRAINT_NAME));
	}
}
