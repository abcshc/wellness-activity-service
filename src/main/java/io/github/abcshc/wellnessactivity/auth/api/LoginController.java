package io.github.abcshc.wellnessactivity.auth.api;

import io.github.abcshc.wellnessactivity.auth.service.LoginCommand;
import io.github.abcshc.wellnessactivity.auth.service.LoginService;
import io.github.abcshc.wellnessactivity.auth.service.RefreshTokenCommand;
import io.github.abcshc.wellnessactivity.auth.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

	private final LoginService loginService;
	private final RefreshTokenService refreshTokenService;

	public LoginController(LoginService loginService, RefreshTokenService refreshTokenService) {
		this.loginService = loginService;
		this.refreshTokenService = refreshTokenService;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(LoginResponse.from(loginService.login(new LoginCommand(
			request.email(),
			request.password()
		))));
	}

	@PostMapping("/refresh")
	public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(RefreshTokenResponse.from(refreshTokenService.refresh(
			new RefreshTokenCommand(request.refreshToken())
		)));
	}
}
