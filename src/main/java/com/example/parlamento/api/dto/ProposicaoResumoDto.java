package com.example.parlamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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
		String identificacao,
		String siglaTipo,
		Integer numero,
		Integer ano,
		String ementa,
		LocalDateTime dataApresentacao,
		String situacao,
		boolean detalheCarregado,
		@JsonProperty("_fonte") FonteDto fonte) {
}
