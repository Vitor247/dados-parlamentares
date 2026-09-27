package com.example.parlamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Envelope de resposta paginada da nossa API. */
public record PaginaDto<T>(
		List<T> conteudo,
		int pagina,
		int tamanho,
		long totalElementos,
		int totalPaginas,
		@JsonProperty("_fonte") Fonte fonte) {

	/** @param atualizadoEm a atualização mais recente entre os itens da página */
	public record Fonte(String origem, LocalDateTime atualizadoEm) {
	}

	public static <E, T> PaginaDto<T> de(Page<E> pagina, Function<E, T> conversor,
			Function<E, LocalDateTime> atualizadoEm) {
		LocalDateTime maisRecente = pagina.getContent().stream()
				.map(atualizadoEm)
				.filter(Objects::nonNull)
				.max(Comparator.naturalOrder())
				.orElse(null);
		return new PaginaDto<>(
				pagina.getContent().stream().map(conversor).toList(),
				pagina.getNumber(),
				pagina.getSize(),
				pagina.getTotalElements(),
				pagina.getTotalPages(),
				new Fonte(FonteDto.ORIGEM_CAMARA, maisRecente));
	}
}
