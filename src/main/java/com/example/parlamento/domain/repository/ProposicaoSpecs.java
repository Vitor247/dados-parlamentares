package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Proposicao;
import com.example.parlamento.domain.entity.ProposicaoAutor;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Filtros opcionais de proposição. Valor null = filtro não aplicado
 * ({@link Specification#unrestricted()}), para compor com {@code allOf}.
 */
public final class ProposicaoSpecs {

	private ProposicaoSpecs() {
	}

	/** Proposições que têm o deputado entre os autores (EXISTS, sem duplicar linhas). */
	public static Specification<Proposicao> deAutoria(Long deputadoId) {
		if (deputadoId == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> {
			Subquery<Long> autoria = query.subquery(Long.class);
			Root<ProposicaoAutor> autor = autoria.from(ProposicaoAutor.class);
			autoria.select(autor.get("id")).where(
					cb.equal(autor.get("proposicao"), root),
					cb.equal(autor.get("deputado").get("id"), deputadoId));
			return cb.exists(autoria);
		};
	}

	public static Specification<Proposicao> doAno(Integer ano) {
		if (ano == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("ano"), ano);
	}

	public static Specification<Proposicao> doNumero(Integer numero) {
		if (numero == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("numero"), numero);
	}

	/** Trecho da ementa, sem diferenciar maiúsculas (ILIKE %valor%). */
	public static Specification<Proposicao> ementaContem(String trecho) {
		return TextoSpecs.contem("ementa", trecho);
	}

	/** Sigla do tipo sem diferenciar maiúsculas ("pl" = "PL"). */
	public static Specification<Proposicao> doTipo(String siglaTipo) {
		if (siglaTipo == null || siglaTipo.isBlank()) {
			return Specification.unrestricted();
		}
		String sigla = siglaTipo.strip().toUpperCase(Locale.ROOT);
		return (root, query, cb) -> cb.equal(root.get("siglaTipo"), sigla);
	}
}
