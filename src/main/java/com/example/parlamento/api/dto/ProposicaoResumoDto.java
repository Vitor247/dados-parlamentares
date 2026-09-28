package com.example.parlamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Proposição em listagens.
 *
 * @param identificacao    como o cidadão conhece a proposição: "PL 1234/2025"
 * @param detalheCarregado false = situação e data de apresentação ainda não foram
 *                         carregadas da fonte (e por isso vêm null)
 */
public record ProposicaoResumoDto(
		Long id,
		@Schema(description = "Como a proposição é conhecida: tipo número/ano", example = "PL 387/2025")
		String identificacao,
		String siglaTipo,
		Integer numero,
		Integer ano,
		String ementa,
		LocalDateTime dataApresentacao,
		@Schema(description = "Situação atual; null enquanto detalheCarregado = false", example = "Aguardando Parecer")
		String situacao,
		@Schema(description = "false = situação e data de apresentação ainda não foram carregadas da fonte "
				+ "(e por isso vêm null); true = proposição enriquecida")
		boolean detalheCarregado,
		@JsonProperty("_fonte") FonteDto fonte) {
}
