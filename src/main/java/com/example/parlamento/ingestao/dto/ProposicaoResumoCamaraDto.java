package com.example.parlamento.ingestao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Item de {@code GET /proposicoes}: só identificação e ementa, sem situação. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProposicaoResumoCamaraDto(
		Long id,
		String uri,
		String siglaTipo,
		Integer codTipo,
		Integer numero,
		Integer ano,
		String ementa) {
}
