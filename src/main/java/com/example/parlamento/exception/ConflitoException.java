package com.example.parlamento.exception;

/** A operação conflita com o estado atual (respondido como 409). */
public class ConflitoException extends RuntimeException {

	public ConflitoException(String mensagem) {
		super(mensagem);
	}
}
