package com.example.parlamento.exception;

/** Recurso inexistente na base (respondido como 404). */
public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String recurso, Object id) {
		super(recurso + " " + id + " não encontrado");
	}
}
