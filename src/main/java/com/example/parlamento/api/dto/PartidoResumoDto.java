package com.example.parlamento.api.dto;

/** Referência compacta a um partido, embutida em outros recursos. */
public record PartidoResumoDto(Long id, String sigla, String nome) {
}
