package com.example.parlamento.ingestao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Item de {@code GET /deputados}. É daqui que sai o vínculo com o partido
 * ({@code uriPartido}), porque no detalhe ele vem null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeputadoResumoCamaraDto(
		Long id,
		String uri,
		String nome,
		String siglaPartido,
		String uriPartido,
		String siglaUf,
		Integer idLegislatura,
		String urlFoto,
		String email) {
}
