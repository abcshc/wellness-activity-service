package io.github.abcshc.wellnessactivity.activity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.abcshc.wellnessactivity.WellnessActivityServiceApplication;
import io.github.abcshc.wellnessactivity.activity.repository.MemberActivityKeyRepository;
import io.github.abcshc.wellnessactivity.activity.repository.StepRecordRepository;
import io.github.abcshc.wellnessactivity.auth.token.repository.RefreshTokenRepository;
import io.github.abcshc.wellnessactivity.member.repository.MemberRepository;
import io.github.abcshc.wellnessactivity.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(classes = WellnessActivityServiceApplication.class)
@AutoConfigureMockMvc
@Import(MySqlTestContainerConfiguration.class)
class ActivityWorkflowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private StepRecordRepository stepRecordRepository;

	@Autowired
	private MemberActivityKeyRepository memberActivityKeyRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private MemberRepository memberRepository;

	@AfterEach
	void tearDown() {
		clearDatabase();
	}

	@BeforeEach
	void setUp() {
		clearDatabase();
	}

	private void clearDatabase() {
		stepRecordRepository.deleteAll();
		memberActivityKeyRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	void 회원가입부터_로그인_업로드_재전송_일월별_조회까지_원본_흐름을_검증한다() throws Exception {
		register("member@example.com");
		String accessToken = login("member@example.com");

		mockMvc.perform(post("/api/v1/activities/steps")
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(activityUploadRequest()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalCount").value(4))
			.andExpect(jsonPath("$.createdCount").value(3))
			.andExpect(jsonPath("$.ignoredCount").value(0))
			.andExpect(jsonPath("$.invalidCount").value(1))
			.andExpect(jsonPath("$.invalidEntries[0].index").value(3));

		mockMvc.perform(post("/api/v1/activities/steps")
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(activityUploadRequest()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.createdCount").value(0))
			.andExpect(jsonPath("$.ignoredCount").value(3))
			.andExpect(jsonPath("$.invalidCount").value(1));

		mockMvc.perform(get("/api/v1/activities/steps/daily")
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.param("recordkey", "record-key-001")
				.param("from", "2024-11-14")
				.param("to", "2024-11-15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].date").value("2024-11-14"))
			.andExpect(jsonPath("$[0].steps").value(70))
			.andExpect(jsonPath("$[0].distanceKm").value(1.2))
			.andExpect(jsonPath("$[0].caloriesKcal").value(6))
			.andExpect(jsonPath("$[1].date").value("2024-11-15"))
			.andExpect(jsonPath("$[1].steps").value(62))
			.andExpect(jsonPath("$[1].distanceKm").value(1.12))
			.andExpect(jsonPath("$[1].caloriesKcal").value(6.2));

		mockMvc.perform(get("/api/v1/activities/steps/monthly")
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.param("recordkey", "record-key-001")
				.param("from", "2024-11")
				.param("to", "2024-11"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].month").value("2024-11"))
			.andExpect(jsonPath("$[0].steps").value(132))
			.andExpect(jsonPath("$[0].distanceKm").value(2.32))
			.andExpect(jsonPath("$[0].caloriesKcal").value(12.2));
	}

	@Test
	void 다른_회원은_업로드된_recordkey를_조회할_수_없다() throws Exception {
		register("owner@example.com");
		String ownerAccessToken = login("owner@example.com");
		mockMvc.perform(post("/api/v1/activities/steps")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerAccessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(activityUploadRequest()))
			.andExpect(status().isOk());
		register("other@example.com");
		String otherAccessToken = login("other@example.com");

		mockMvc.perform(get("/api/v1/activities/steps/daily")
				.header(HttpHeaders.AUTHORIZATION, bearer(otherAccessToken))
				.param("recordkey", "record-key-001")
				.param("from", "2024-11-14")
				.param("to", "2024-11-15"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACTIVITY_RECORD_KEY_FORBIDDEN"));
	}

	private void register(String email) throws Exception {
		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "홍길동",
					  "nickname": "길동이",
					  "email": "%s",
					  "password": "password"
					}
					""".formatted(email)))
			.andExpect(status().isCreated());
	}

	private String login(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"email":"%s","password":"password"}
					""".formatted(email)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("accessToken").asString();
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}

	private String activityUploadRequest() {
		return """
			{
			  "recordkey": "record-key-001",
			  "data": {
			    "source": {"name": "SamsungHealth"},
			    "entries": [
			      {
			        "period": {"from": "2024-11-14T23:30:00+0900", "to": "2024-11-15T00:30:00+0900"},
			        "steps": 100,
			        "distance": {"unit": "km", "value": 2},
			        "calories": {"unit": "kcal", "value": 10}
			      },
			      {
			        "period": {"from": "2024-11-15T00:00:00+0900", "to": "2024-11-15T00:00:00+0900"},
			        "steps": 12,
			        "distance": {"unit": "km", "value": 0.12},
			        "calories": {"unit": "kcal", "value": 1.2}
			      },
			      {
			        "period": {"from": "2024-11-14T23:30:00+0900", "to": "2024-11-14T23:40:00+0900"},
			        "steps": 20,
			        "distance": {"unit": "km", "value": 0.2},
			        "calories": {"unit": "kcal", "value": 1}
			      },
			      {
			        "period": {"from": "2024-11-15T01:00:00+0900", "to": "2024-11-15T00:50:00+0900"},
			        "steps": 1,
			        "distance": {"unit": "km", "value": 0.01},
			        "calories": {"unit": "kcal", "value": 0.1}
			      }
			    ]
			  }
			}
			""";
	}
}
