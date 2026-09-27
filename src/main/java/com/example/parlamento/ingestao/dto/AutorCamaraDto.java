package com.example.parlamento.ingestao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Item de {@code GET /proposicoes/{id}/autores}. Não há campo {@code id}:
 * quando o autor é deputado, o id está no final da {@code uri}, que pode vir null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutorCamaraDto(
		String uri,
		String nome,
		Integer codTipo,
		String tipo,
		Integer ordemAssinatura,
		Integer proponente) {
}
