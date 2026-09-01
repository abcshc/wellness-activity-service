package io.github.abcshc.wellnessactivity.auth.config;

import io.github.abcshc.wellnessactivity.auth.security.CurrentMemberIdArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthenticationWebMvcConfiguration implements WebMvcConfigurer {

	private final CurrentMemberIdArgumentResolver currentMemberIdArgumentResolver;

	public AuthenticationWebMvcConfiguration(CurrentMemberIdArgumentResolver currentMemberIdArgumentResolver) {
		this.currentMemberIdArgumentResolver = currentMemberIdArgumentResolver;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentMemberIdArgumentResolver);
	}
}
