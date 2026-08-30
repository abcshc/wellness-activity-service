package io.github.abcshc.wellnessactivity.auth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.abcshc.wellnessactivity.auth.security.JsonAccessDeniedHandler;
import io.github.abcshc.wellnessactivity.auth.security.JsonAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtSecurityConfiguration {

	@Bean
	public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
		return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtProperties.secretKey()));
	}

	@Bean
	public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(jwtProperties.secretKey())
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()));
		return jwtDecoder;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		JsonAuthenticationEntryPoint authenticationEntryPoint,
		JsonAccessDeniedHandler accessDeniedHandler
	) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.POST,
					"/api/v1/members",
					"/api/v1/auth/login",
					"/api/v1/auth/refresh",
					"/api/v1/auth/logout"
				).permitAll()
				.anyRequest().authenticated()
			)
			.exceptionHandling(exceptionHandling -> exceptionHandling
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			)
			.oauth2ResourceServer(resourceServer -> resourceServer
				.jwt(Customizer.withDefaults())
				.authenticationEntryPoint(authenticationEntryPoint)
			);

		return http.build();
	}

}
