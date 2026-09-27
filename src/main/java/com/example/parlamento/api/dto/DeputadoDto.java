package com.example.parlamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * Deputado completo. Campos não publicados pela fonte vêm null, nunca omitidos.
 *
 * @param totalProposicoes contagem das proposições de autoria do deputado que estão
 *                         na nossa base (dentro do recorte importado). Não é métrica
 *                         de produtividade parlamentar.
 */
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
		long totalProposicoes,
		@JsonProperty("_fonte") FonteDto fonte) {
}
