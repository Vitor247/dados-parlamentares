package com.example.parlamento.domain.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextoSpecsTest {

	@Test
	void escapaCuringasDoLikeParaBuscaLiteral() {
		assertThat(TextoSpecs.escaparLike("100%")).isEqualTo("100\\%");
		assertThat(TextoSpecs.escaparLike("a_b")).isEqualTo("a\\_b");
		assertThat(TextoSpecs.escaparLike("c:\\x")).isEqualTo("c:\\\\x");
	}

	@Test
	void textoSemCuringasFicaIgual() {
		assertThat(TextoSpecs.escaparLike("saúde")).isEqualTo("saúde");
	}
}
