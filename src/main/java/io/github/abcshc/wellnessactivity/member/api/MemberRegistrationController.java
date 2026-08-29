package io.github.abcshc.wellnessactivity.member.api;

import io.github.abcshc.wellnessactivity.member.service.MemberRegistrationCommand;
import io.github.abcshc.wellnessactivity.member.service.MemberRegistrationResult;
import io.github.abcshc.wellnessactivity.member.service.MemberRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberRegistrationController {

	private final MemberRegistrationService memberRegistrationService;

	public MemberRegistrationController(MemberRegistrationService memberRegistrationService) {
		this.memberRegistrationService = memberRegistrationService;
	}

	@PostMapping
	public ResponseEntity<MemberRegistrationResponse> register(@Valid @RequestBody MemberRegistrationRequest request) {
		MemberRegistrationResult result = memberRegistrationService.register(new MemberRegistrationCommand(
			request.name(),
			request.nickname(),
			request.email(),
			request.password()
		));

		return ResponseEntity.status(HttpStatus.CREATED).body(MemberRegistrationResponse.from(result));
	}
}
