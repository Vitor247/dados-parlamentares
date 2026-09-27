package com.example.parlamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Deputado em listagens.
 *
 * @param siglaPartido sigla como publicada pela fonte (retrato)
 * @param partido      partido resolvido na nossa base; null se não pôde ser vinculado
 */
public record DeputadoResumoDto(
		Long id,
		String nome,
		String siglaUf,
		String siglaPartido,
		PartidoResumoDto partido,
		String situacao,
		String urlFoto,
		@JsonProperty("_fonte") FonteDto fonte) {
}
