package com.example.parlamento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração do acesso à API de Dados Abertos da Câmara ({@code camara.api.*}).
 */
@ConfigurationProperties(prefix = "camara.api")
public record CamaraProperties(
		String baseUrl,
		Duration connectTimeout,
		Duration readTimeout,
		int itensPorPagina,
		Retry retry) {

	/**
	 * Backoff exponencial aplicado em 429, 5xx e falhas de I/O.
	 *
	 * @param maxRetentativas quantas vezes repetir depois da primeira tentativa
	 */
	public record Retry(
			long maxRetentativas,
			Duration atrasoInicial,
			double multiplicador,
			Duration atrasoMaximo) {
	}
}
