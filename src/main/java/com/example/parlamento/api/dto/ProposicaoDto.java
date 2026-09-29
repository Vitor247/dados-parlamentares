package com.example.parlamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Proposição completa.
 *
 * @param detalheCarregado false = só os dados da lista foram importados; então
 *                         {@code dataApresentacao}, {@code urlInteiroTeor} e
 *                         {@code situacao} vêm null porque ainda não foram carregados
 * @param situacao         situação atual (não o histórico de tramitação); null
 *                         enquanto o detalhe não foi carregado
 */
public record ProposicaoDto(
		Long id,
		String identificacao,
		String siglaTipo,
		Integer codTipo,
		String descricaoTipo,
		Integer numero,
		Integer ano,
		String ementa,
		LocalDateTime dataApresentacao,
		String urlInteiroTeor,
		@Schema(description = "Situação atual (não o histórico de tramitação); null enquanto detalheCarregado = false")
		SituacaoDto situacao,
		@Schema(description = "false = só identificação e ementa foram importadas; dataApresentacao, "
				+ "urlInteiroTeor e situacao ainda não foram carregadas e vêm null")
		boolean detalheCarregado,
		@JsonProperty("_fonte") FonteDto fonte) {

	/**
	 * Status atual da proposição conforme publicado pela fonte.
	 *
	 * @param data        data do último evento de tramitação, segundo a Câmara
	 * @param atualizadaEm quando esta situação foi buscada na fonte — a situação pode ter
	 *                    mudado na Câmara depois disso
	 */
	public record SituacaoDto(
			String descricao,
			Integer codigo,
			LocalDateTime data,
			String orgaoSigla,
			String ultimaTramitacao,
			@Schema(description = "Quando esta situação foi consultada na fonte. A situação é revalidada "
					+ "periodicamente e pode ter mudado na Câmara desde então.")
			LocalDateTime atualizadaEm) {
	}
}
