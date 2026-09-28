package com.example.parlamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Autoria de uma proposição.
 *
 * @param detalheCarregado false = a autoria completa ainda não foi carregada: a lista
 *                         contém só os deputados da base que constam como autores,
 *                         sem coautores externos, ordem de assinatura ou proponente
 */
public record AutoresDto(
		Long proposicaoId,
		String identificacao,
		@Schema(description = "false = autoria completa ainda não carregada: só os deputados da base que constam "
				+ "como autores, sem coautores externos, ordem de assinatura ou proponente")
		boolean detalheCarregado,
		List<AutorDto> autores,
		@JsonProperty("_fonte") FonteDto fonte) {

	/**
	 * @param tipo     tipo de autor conforme a fonte ("Deputado(a)", "Órgão do Poder Executivo"...)
	 * @param deputado deputado na nossa base; null quando o autor não é deputado ou
	 *                 não está na base (ex.: deputado de outra legislatura)
	 */
	public record AutorDto(
			String nome,
			String tipo,
			Integer ordemAssinatura,
			Boolean proponente,
			@Schema(description = "Deputado na base; null quando o autor não é deputado ou não está na base")
			DeputadoRefDto deputado) {
	}

	/** Referência compacta ao deputado autor. */
	public record DeputadoRefDto(Long id, String nome, String siglaPartido, String siglaUf) {
	}
}
