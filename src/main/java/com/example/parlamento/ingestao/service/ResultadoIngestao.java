package com.example.parlamento.ingestao.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumo de uma execução de ingestão, devolvido pelo endpoint administrativo.
 *
 * @param proposicoesPendentes     proposições que nunca foram enriquecidas (Fase B)
 * @param proposicoesDesatualizadas proposições enriquecidas cuja situação passou da validade
 *                                  configurada e será buscada de novo
 */
public record ResultadoIngestao(
		LocalDateTime iniciadoEm,
		LocalDateTime finalizadoEm,
		List<Etapa> etapas,
		long proposicoesPendentes,
		long proposicoesDesatualizadas) {

	/**
	 * @param processados unidades gravadas com sucesso
	 * @param falhas      unidades que falharam (detalhe em {@code ingestao_log})
	 */
	public record Etapa(String recurso, int processados, int falhas) {
	}
}
