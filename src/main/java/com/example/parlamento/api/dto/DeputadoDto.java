package com.example.parlamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

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
		@Schema(description = "Sigla do partido como publicada pela fonte (retrato)", example = "PSD")
		String siglaPartido,
		@Schema(description = "Partido resolvido na base; null se não pôde ser vinculado")
		PartidoResumoDto partido,
		String situacao,
		String condicaoEleitoral,
		String email,
		String urlFoto,
		LocalDate dataNascimento,
		String ufNascimento,
		String municipioNascimento,
		String escolaridade,
		@Schema(description = "Contagem das proposições de autoria do deputado que estão na base, dentro do recorte "
				+ "importado. Não é métrica de produtividade parlamentar.", example = "16")
		long totalProposicoes,
		@JsonProperty("_fonte") FonteDto fonte) {
}
