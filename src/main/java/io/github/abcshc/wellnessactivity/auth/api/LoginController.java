package io.github.abcshc.wellnessactivity.auth.api;

import io.github.abcshc.wellnessactivity.auth.service.LoginCommand;
import io.github.abcshc.wellnessactivity.auth.service.LoginService;
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

	public LoginController(LoginService loginService) {
		this.loginService = loginService;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(LoginResponse.from(loginService.login(new LoginCommand(
			request.email(),
			request.password()
		))));
	}
}
