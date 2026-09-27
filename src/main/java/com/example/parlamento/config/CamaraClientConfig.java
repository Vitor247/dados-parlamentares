package com.example.parlamento.config;

import com.example.parlamento.ingestao.client.CamaraClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryState;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties({CamaraProperties.class, IngestaoProperties.class})
public class CamaraClientConfig {

	private static final Logger log = LoggerFactory.getLogger(CamaraClientConfig.class);

	@Bean
	RestClient camaraRestClient(RestClient.Builder builder, CamaraProperties props) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(props.connectTimeout())
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(props.readTimeout());

		return builder
				.baseUrl(props.baseUrl())
				.requestFactory(requestFactory)
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	@Bean
	RetryTemplate camaraRetryTemplate(CamaraProperties props) {
		CamaraProperties.Retry retry = props.retry();
		RetryPolicy policy = RetryPolicy.builder()
				.maxRetries(retry.maxRetentativas())
				.delay(retry.atrasoInicial())
				.multiplier(retry.multiplicador())
				.maxDelay(retry.atrasoMaximo())
				.predicate(CamaraClient::deveRetentar)
				.build();

		RetryTemplate template = new RetryTemplate(policy);
		template.setRetryListener(new RetryListener() {
			@Override
			public void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable, RetryState state) {
				log.warn("Câmara: {} falhou ({}); retentativa {} de {}",
						retryable.getName(), state.getLastException().getMessage(),
						state.getRetryCount(), retry.maxRetentativas());
			}
		});
		return template;
	}
}
