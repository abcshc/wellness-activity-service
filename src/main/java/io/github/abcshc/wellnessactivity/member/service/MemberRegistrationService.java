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
	 * 동시 회원가입에서 발생한 명시적 이메일 유니크 제약조건만 클라이언트가 해결할 수 있는 중복 오류로 변환한다.
	 */
	private boolean isEmailUniqueConstraintViolation(DataIntegrityViolationException exception) {
		Throwable cause = exception;

		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintViolation
				&& "uk_members_email".equals(constraintViolation.getConstraintName())) {
				return true;
			}
			cause = cause.getCause();
		}

		return false;
	}
}
