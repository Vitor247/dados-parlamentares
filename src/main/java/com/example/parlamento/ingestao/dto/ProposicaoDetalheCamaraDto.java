package com.example.parlamento.ingestao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code GET /proposicoes/{id}}. Datas vêm sem segundos ("2021-04-09T18:16")
 * e ficam como texto; a conversão tolerante é feita no mapper.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProposicaoDetalheCamaraDto(
		Long id,
		String uri,
		String siglaTipo,
		Integer codTipo,
		Integer numero,
		Integer ano,
		String ementa,
		String dataApresentacao,
		String descricaoTipo,
		String urlInteiroTeor,
		StatusProposicao statusProposicao) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record StatusProposicao(
			String dataHora,
			String siglaOrgao,
			String descricaoTramitacao,
			String descricaoSituacao,
			Integer codSituacao,
			String despacho,
			String ambito) {
	}
}
