package com.example.parlamento.ingestao.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DatasCamaraTest {

	@Test
	void converteData() {
		assertThat(DatasCamara.data("1976-03-03")).isEqualTo(LocalDate.of(1976, 3, 3));
	}

	@Test
	void converteDataHoraSemSegundosComoAFontePublica() {
		assertThat(DatasCamara.dataHora("2021-04-09T18:16")).isEqualTo(LocalDateTime.of(2021, 4, 9, 18, 16));
	}

	@Test
	void converteDataHoraComSegundos() {
		assertThat(DatasCamara.dataHora("2021-04-09T18:16:42")).isEqualTo(LocalDateTime.of(2021, 4, 9, 18, 16, 42));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"  ", "03/03/1976", "1976-13-40", "ontem"})
	void dataAusenteOuMalformadaViraNull(String valor) {
		assertThat(DatasCamara.data(valor)).isNull();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"  ", "2021-04-09", "09/04/2021 18:16", "2021-04-09T25:00"})
	void dataHoraAusenteOuMalformadaViraNull(String valor) {
		assertThat(DatasCamara.dataHora(valor)).isNull();
	}
}
