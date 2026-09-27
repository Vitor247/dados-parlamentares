package com.example.parlamento.ingestao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Item de {@code GET /partidos}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PartidoCamaraDto(Long id, String sigla, String nome, String uri) {
}
