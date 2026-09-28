package com.example.parlamento.exception;

import org.springframework.http.HttpStatus;

/** Acesso a endpoint protegido recusado: 401 (chave ausente/errada) ou 403 (área desabilitada). */
public class AcessoNegadoException extends RuntimeException {

	private final HttpStatus status;

	private AcessoNegadoException(HttpStatus status, String mensagem) {
		super(mensagem);
		this.status = status;
	}

	public static AcessoNegadoException chaveInvalida() {
		return new AcessoNegadoException(HttpStatus.UNAUTHORIZED, "Chave de administração ausente ou inválida");
	}

	public static AcessoNegadoException administracaoDesabilitada() {
		return new AcessoNegadoException(HttpStatus.FORBIDDEN,
				"Administração desabilitada: nenhuma chave configurada no servidor (ADMIN_API_KEY)");
	}

	public HttpStatus getStatus() {
		return status;
	}
}
