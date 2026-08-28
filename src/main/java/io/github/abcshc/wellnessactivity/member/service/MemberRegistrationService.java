package io.github.abcshc.wellnessactivity.member.service;

import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;

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
			throw new DuplicateEmailException();
		}

		String passwordHash = passwordHasher.hash(command.password());
		MemberEntity member = new MemberEntity(
			command.name(),
			command.nickname(),
			normalizedEmail,
			passwordHash
		);

		try {
			// 동시 요청으로 인한 유니크 제약조건 오류를 이 유스케이스 안에서 변환한다.
			memberRepository.saveAndFlush(member);
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateEmailException();
		}

		return new MemberRegistrationResult(command.name(), command.nickname(), normalizedEmail);
	}

	private String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}
}
