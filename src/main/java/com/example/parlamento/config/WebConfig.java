package com.example.parlamento.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(SegurancaProperties.class)
public class WebConfig implements WebMvcConfigurer {

	private final SegurancaProperties props;

	public WebConfig(SegurancaProperties props) {
		this.props = props;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new AdminApiKeyInterceptor(props)).addPathPatterns("/api/v1/admin/**");
	}

	/** O frontend roda em outro domínio; só a API pública de leitura é liberada para ele. */
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/v1/**")
				.allowedOrigins(props.corsOrigens().toArray(String[]::new))
				.allowedMethods("GET")
				.maxAge(3600);
	}
}
