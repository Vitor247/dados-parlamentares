package com.example.parlamento.ingestao.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UriCamaraTest {

	@Test
	void extraiIdDoUltimoSegmento() {
		assertThat(UriCamara.extrairId("https://dadosabertos.camara.leg.br/api/v2/deputados/204531"))
				.contains(204531L);
	}

	@Test
	void toleraBarraFinalEEspacos() {
		assertThat(UriCamara.extrairId("  https://dadosabertos.camara.leg.br/api/v2/partidos/36899/  "))
				.contains(36899L);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "https://dadosabertos.camara.leg.br/api/v2/orgaos/abc", "sem-barra", "/"})
	void uriAusenteOuSemIdNumericoResultaVazio(String uri) {
		assertThat(UriCamara.extrairId(uri)).isEmpty();
	}
}
