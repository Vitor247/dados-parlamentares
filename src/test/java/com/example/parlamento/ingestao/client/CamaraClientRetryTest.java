package com.example.parlamento.ingestao.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** Só vale repetir o que é transitório: 429, 5xx e falha de rede. */
class CamaraClientRetryTest {

	@Test
	void repeteEm429() {
		assertThat(CamaraClient.deveRetentar(erroHttp(HttpStatus.TOO_MANY_REQUESTS))).isTrue();
	}

	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "BAD_GATEWAY", "SERVICE_UNAVAILABLE", "GATEWAY_TIMEOUT"})
	void repeteEm5xx(HttpStatus status) {
		assertThat(CamaraClient.deveRetentar(erroHttp(status))).isTrue();
	}

	@Test
	void repeteEmFalhaDeRedeOuTimeout() {
		assertThat(CamaraClient.deveRetentar(new ResourceAccessException("timeout", new IOException()))).isTrue();
	}

	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "NOT_FOUND", "FORBIDDEN"})
	void naoRepeteEmOutros4xx(HttpStatus status) {
		assertThat(CamaraClient.deveRetentar(erroHttp(status))).isFalse();
	}

	@Test
	void naoRepeteErroDeProgramacao() {
		assertThat(CamaraClient.deveRetentar(new IllegalStateException())).isFalse();
	}

	private static RuntimeException erroHttp(HttpStatus status) {
		return status.is4xxClientError()
				? HttpClientErrorException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY, null, null)
				: HttpServerErrorException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY, null, null);
	}
}
