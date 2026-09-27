package com.example.parlamento.exception;

/** A API da Câmara não respondeu de forma utilizável, mesmo após as retentativas. */
public class FonteExternaIndisponivelException extends RuntimeException {

	public FonteExternaIndisponivelException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
