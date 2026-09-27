package com.example.parlamento.ingestao.service;

import java.time.LocalDateTime;
import java.util.List;

/** Resumo de uma execução de ingestão, devolvido pelo endpoint administrativo. */
public record ResultadoIngestao(
		LocalDateTime iniciadoEm,
		LocalDateTime finalizadoEm,
		List<Etapa> etapas) {

	/**
	 * @param processados registros gravados com sucesso
	 * @param falhas      unidades que falharam (detalhe em {@code ingestao_log})
	 */
	public record Etapa(String recurso, int processados, int falhas) {
	}
}
