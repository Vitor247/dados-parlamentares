package com.example.parlamento.api.dto;

import com.example.parlamento.domain.repository.SituacaoContagem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Situações presentes num conjunto de proposições, para alimentar o filtro por situação. */
public record SituacoesDto(List<Item> situacoes) {

	/**
	 * @param descricao texto oficial da situação; é o valor a usar no filtro {@code situacao}
	 * @param total     proposições do conjunto nesta situação (fotografia da última consulta à fonte)
	 */
	public record Item(
			@Schema(example = "Aguardando Parecer") String descricao,
			@Schema(example = "3579") long total) {
	}

	/**
	 * Em ordem alfabética com regras do português: a ordenação do banco é byte a byte e poria
	 * acentuadas e minúsculas fora de lugar.
	 */
	public static SituacoesDto de(List<SituacaoContagem> contagens) {
		Collator portugues = Collator.getInstance(Locale.of("pt", "BR"));
		return new SituacoesDto(contagens.stream()
				.map(c -> new Item(c.descricao(), c.total()))
				.sorted(Comparator.comparing(Item::descricao, portugues))
				.toList());
	}
}
