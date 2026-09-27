package com.example.parlamento.domain.repository;

import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Filtros textuais reutilizáveis entre entidades. */
final class TextoSpecs {

	private TextoSpecs() {
	}

	/**
	 * {@code atributo ILIKE %trecho%}. Os curingas do LIKE digitados pelo usuário
	 * ("%", "_") são escapados e buscados literalmente. Trecho vazio = sem filtro.
	 */
	static <T> Specification<T> contem(String atributo, String trecho) {
		if (trecho == null || trecho.isBlank()) {
			return Specification.unrestricted();
		}
		String padrao = "%" + escaparLike(trecho.strip().toLowerCase(Locale.ROOT)) + "%";
		return (root, query, cb) -> cb.like(cb.lower(root.get(atributo)), padrao, '\\');
	}

	static String escaparLike(String valor) {
		return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
