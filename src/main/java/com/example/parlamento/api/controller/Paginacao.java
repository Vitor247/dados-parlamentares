package com.example.parlamento.api.controller;

/**
 * Tamanho de página padrão da API. Precisa ser informado em todo {@code @PageableDefault}:
 * o atributo {@code size} da anotação tem default 10 e prevalece sobre
 * {@code spring.data.web.pageable.default-page-size}.
 */
final class Paginacao {

	static final int TAMANHO_PADRAO = 20;

	private Paginacao() {
	}
}
