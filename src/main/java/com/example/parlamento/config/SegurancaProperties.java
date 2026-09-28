package com.example.parlamento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Exposição pública da API ({@code parlamento.seguranca.*}).
 *
 * @param adminApiKey chave exigida no header {@code X-Admin-Key} dos endpoints /admin.
 *                    Vazia = administração bloqueada (nunca aberta por esquecimento).
 * @param corsOrigens origens autorizadas a chamar a API pelo navegador (o frontend)
 */
@ConfigurationProperties(prefix = "parlamento.seguranca")
public record SegurancaProperties(String adminApiKey, List<String> corsOrigens) {

	public boolean adminHabilitado() {
		return adminApiKey != null && !adminApiKey.isBlank();
	}
}
