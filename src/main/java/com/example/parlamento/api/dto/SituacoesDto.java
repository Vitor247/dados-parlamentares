package com.example.parlamento.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Situações presentes na base, para alimentar o filtro por situação. */
public record SituacoesDto(List<Item> situacoes) {

	/**
	 * @param descricao texto oficial da situação; é o valor a usar no filtro {@code situacao}
	 * @param total     proposições na base nesta situação (fotografia da última consulta à fonte)
	 */
	public record Item(
			@Schema(example = "Aguardando Parecer") String descricao,
			@Schema(example = "3579") long total) {
	}
}
