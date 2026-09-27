package com.example.parlamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartidoDto(
		Long id,
		String sigla,
		String nome,
		@JsonProperty("_fonte") FonteDto fonte) {
}
