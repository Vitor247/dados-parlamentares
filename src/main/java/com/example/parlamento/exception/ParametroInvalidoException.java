package com.example.parlamento.exception;

/** Parâmetro de entrada com valor fora do aceito (respondido como 400). */
public class ParametroInvalidoException extends RuntimeException {

	public ParametroInvalidoException(String mensagem) {
		super(mensagem);
	}
}
