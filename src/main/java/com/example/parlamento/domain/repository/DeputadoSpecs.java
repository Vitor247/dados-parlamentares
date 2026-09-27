package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.entity.Partido;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Filtros opcionais de deputado. Valor null/vazio = filtro não aplicado. */
public final class DeputadoSpecs {

	private DeputadoSpecs() {
	}

	public static Specification<Deputado> daUf(String uf) {
		if (uf == null || uf.isBlank()) {
			return Specification.unrestricted();
		}
		String sigla = uf.strip().toUpperCase(Locale.ROOT);
		return (root, query, cb) -> cb.equal(root.get("siglaUf"), sigla);
	}

	/**
	 * Filtra pela sigla do partido (o que o usuário conhece), resolvida para
	 * {@code partido_id}. Como siglas podem se repetir entre partidos ao longo do tempo,
	 * a resolução aceita todos os ids com aquela sigla. Sigla inexistente = nenhum resultado.
	 */
	public static Specification<Deputado> doPartido(String siglaPartido) {
		if (siglaPartido == null || siglaPartido.isBlank()) {
			return Specification.unrestricted();
		}
		String sigla = siglaPartido.strip().toUpperCase(Locale.ROOT);
		return (root, query, cb) -> {
			Subquery<Long> ids = query.subquery(Long.class);
			Root<Partido> partido = ids.from(Partido.class);
			ids.select(partido.get("id")).where(cb.equal(cb.upper(partido.get("sigla")), sigla));
			return root.get("partido").get("id").in(ids);
		};
	}

	public static Specification<Deputado> nomeContem(String trecho) {
		return TextoSpecs.contem("nome", trecho);
	}
}
