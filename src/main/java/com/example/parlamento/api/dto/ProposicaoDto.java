package com.example.parlamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Proposição completa.
 *
 * @param detalheCarregado false = só os dados da lista foram importados; então
 *                         {@code dataApresentacao}, {@code urlInteiroTeor} e
 *                         {@code situacao} vêm null porque ainda não foram carregados
 * @param situacao         situação atual (não o histórico de tramitação); null
 *                         enquanto o detalhe não foi carregado
 */
public record ProposicaoDto(
		Long id,
		String identificacao,
		String siglaTipo,
		Integer codTipo,
		String descricaoTipo,
		Integer numero,
		Integer ano,
		String ementa,
		LocalDateTime dataApresentacao,
		String urlInteiroTeor,
		SituacaoDto situacao,
		boolean detalheCarregado,
		@JsonProperty("_fonte") FonteDto fonte) {

	/** Status atual da proposição conforme publicado pela fonte. */
	public record SituacaoDto(
			String descricao,
			Integer codigo,
			LocalDateTime data,
			String orgaoSigla,
			String ultimaTramitacao) {
	}
}
