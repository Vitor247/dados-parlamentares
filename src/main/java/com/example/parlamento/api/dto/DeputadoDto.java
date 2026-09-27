package com.example.parlamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/** Deputado completo. Campos não publicados pela fonte vêm null, nunca omitidos. */
public record DeputadoDto(
		Long id,
		String nome,
		String nomeCivil,
		String siglaUf,
		Integer idLegislatura,
		String siglaPartido,
		PartidoResumoDto partido,
		String situacao,
		String condicaoEleitoral,
		String email,
		String urlFoto,
		LocalDate dataNascimento,
		String ufNascimento,
		String municipioNascimento,
		String escolaridade,
		@JsonProperty("_fonte") FonteDto fonte) {
}
