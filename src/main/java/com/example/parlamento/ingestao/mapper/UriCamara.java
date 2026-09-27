package com.example.parlamento.ingestao.mapper;

import java.util.Optional;

/** Utilitário para URIs da API da Câmara, onde o id do recurso é o último segmento. */
public final class UriCamara {

	private UriCamara() {
	}

	/**
	 * Extrai o id numérico do final da URI
	 * ({@code https://.../deputados/204531} → {@code 204531}).
	 * Vazio se a URI for nula, vazia ou não terminar em número.
	 */
	public static Optional<Long> extrairId(String uri) {
		if (uri == null || uri.isBlank()) {
			return Optional.empty();
		}
		String semBarraFinal = uri.strip().replaceAll("/+$", "");
		String ultimoSegmento = semBarraFinal.substring(semBarraFinal.lastIndexOf('/') + 1);
		try {
			return Optional.of(Long.parseLong(ultimoSegmento));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
