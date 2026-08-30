package io.github.abcshc.wellnessactivity.auth.api;

import io.github.abcshc.wellnessactivity.member.api.validation.PasswordMaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
	String email,
	@NotBlank(message = "비밀번호는 필수입니다.")
	@PasswordMaxBytes
	String password
) {
}
