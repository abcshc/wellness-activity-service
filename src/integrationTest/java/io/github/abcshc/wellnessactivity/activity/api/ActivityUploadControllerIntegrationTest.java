package io.github.abcshc.wellnessactivity.activity.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import io.github.abcshc.wellnessactivity.auth.token.JwtTokenIssuer;
import io.github.abcshc.wellnessactivity.member.entity.MemberEntity;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@AutoConfigureMockMvc
@Import(MySqlTestContainerConfiguration.class)
class ActivityUploadControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenIssuer jwtTokenIssuer;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberActivityKeyRepository memberActivityKeyRepository;

	@Autowired
	private StepRecordRepository stepRecordRepository;

	@AfterEach
	void tearDown() {
		stepRecordRepository.deleteAll();
		memberActivityKeyRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	void 유효_항목과_중복_무효_항목을_함께_보내면_부분_성공_결과를_반환한다() throws Exception {
		MemberEntity member = savedMember("member@example.com");

		mockMvc.perform(post("/api/v1/activities/steps")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(member))
				.contentType(MediaType.APPLICATION_JSON)
				.content(uploadRequest("""
					{
					  "period": {"from": "2024-11-15 09:00:00", "to": "2024-11-15 09:10:00"},
					  "steps": 32,
					  "distance": {"unit": "km", "value": 0.02422},
					  "calories": {"unit": "kcal", "value": 1.21}
					}
					""", """
					{
					  "period": {"from": "2024-11-15 09:00:00", "to": "2024-11-15 09:10:00"},
					  "steps": 32,
					  "distance": {"unit": "km", "value": 0.02422},
					  "calories": {"unit": "kcal", "value": 1.21}
					}
					""", """
					{
					  "period": {"from": "2024-11-15 10:00:00", "to": "2024-11-15 09:10:00"},
					  "steps": 12,
					  "distance": {"unit": "km", "value": 0.01},
					  "calories": {"unit": "kcal", "value": 0.5}
					}
					""")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalCount").value(3))
			.andExpect(jsonPath("$.createdCount").value(1))
			.andExpect(jsonPath("$.ignoredCount").value(1))
			.andExpect(jsonPath("$.invalidCount").value(1))
			.andExpect(jsonPath("$.invalidEntries[0].index").value(2))
			.andExpect(jsonPath("$.invalidEntries[0].field").value("period.to"))
			.andExpect(jsonPath("$.invalidEntries[0].code").value("ACTIVITY_INVALID_PERIOD"));
	}

	@Test
	void recordkey가_없으면_공통_400_오류를_반환한다() throws Exception {
		MemberEntity member = savedMember("member@example.com");

		mockMvc.perform(post("/api/v1/activities/steps")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(member))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"recordkey":"","data":{"source":{"name":"SamsungHealth"},"entries":[]}}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ACTIVITY_INVALID_RECORD_KEY"))
			.andExpect(jsonPath("$.path").value("/api/v1/activities/steps"));
	}

	@Test
	void JSON_본문을_해석할_수_없으면_400_공통_오류를_반환한다() throws Exception {
		MemberEntity member = savedMember("member@example.com");

		mockMvc.perform(post("/api/v1/activities/steps")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(member))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.path").value("/api/v1/activities/steps"));
	}

	@Test
	void 다른_회원에게_연결된_recordkey는_403_오류를_반환한다() throws Exception {
		MemberEntity owner = savedMember("owner@example.com");
		MemberEntity otherMember = savedMember("other@example.com");
		String request = uploadRequest(validEntry());

		mockMvc.perform(post("/api/v1/activities/steps")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/activities/steps")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(otherMember))
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACTIVITY_RECORD_KEY_FORBIDDEN"));
	}

	@Test
	void 인증되지_않은_요청은_401_오류를_반환한다() throws Exception {
		mockMvc.perform(post("/api/v1/activities/steps")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	private MemberEntity savedMember(String email) {
		return memberRepository.saveAndFlush(new MemberEntity("홍길동", "길동이", email, "password-hash"));
	}

	private String bearerToken(MemberEntity member) {
		return "Bearer " + jwtTokenIssuer.issue(member.getId()).value();
	}

	private String uploadRequest(String... entries) {
		return """
			{
			  "recordkey": "record-key-001",
			  "type": "steps",
			  "data": {
			    "source": {"name": "SamsungHealth"},
			    "entries": [%s]
			  }
			}
			""".formatted(String.join(",", entries));
	}

	private String validEntry() {
		return """
			{
			  "period": {"from": "2024-11-15 09:00:00", "to": "2024-11-15 09:10:00"},
			  "steps": 32,
			  "distance": {"unit": "km", "value": 0.02422},
			  "calories": {"unit": "kcal", "value": 1.21}
			}
			""";
	}
}
