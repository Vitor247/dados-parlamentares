package com.example.parlamento.ingestao.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Conversão tolerante das datas da fonte. Valor ausente ou malformado vira null:
 * a ausência é informação, e um campo ruim não deve derrubar o registro inteiro.
 */
public final class DatasCamara {

	private DatasCamara() {
	}

	/** {@code "1976-03-03"} → LocalDate. */
	public static LocalDate data(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(valor.strip());
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/** {@code "2021-04-09T18:16"} (segundos opcionais) → LocalDateTime. */
	public static LocalDateTime dataHora(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		try {
			return LocalDateTime.parse(valor.strip());
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
