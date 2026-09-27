package com.example.parlamento.ingestao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code GET /deputados/{id}}. O campo {@code cpf} existe na fonte mas não é
 * mapeado de propósito: é PII sem função no produto.
 * Datas ficam como texto; a conversão tolerante é feita no mapper.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeputadoDetalheCamaraDto(
		Long id,
		String uri,
		String nomeCivil,
		UltimoStatus ultimoStatus,
		String dataNascimento,
		String dataFalecimento,
		String ufNascimento,
		String municipioNascimento,
		String escolaridade) {

	/** Atenção: {@code uriPartido} costuma vir null aqui mesmo com a sigla preenchida. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record UltimoStatus(
			String nome,
			String siglaPartido,
			String uriPartido,
			String siglaUf,
			Integer idLegislatura,
			String urlFoto,
			String email,
			String situacao,
			String condicaoEleitoral) {
	}
}
