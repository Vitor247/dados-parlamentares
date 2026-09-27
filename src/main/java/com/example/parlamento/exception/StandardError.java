package com.example.parlamento.exception;

import java.time.OffsetDateTime;

/** Corpo padrão de toda resposta de erro da API. */
public record StandardError(
		OffsetDateTime timestamp,
		int status,
		String erro,
		String mensagem,
		String caminho) {
}
