package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Proposicao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Agregação com Criteria API reaproveitando as mesmas Specifications da listagem: o filtro
 * da contagem é, por construção, idêntico ao da busca — sem duplicar regra em JPQL.
 */
class ProposicaoRepositoryCustomImpl implements ProposicaoRepositoryCustom {

	private final EntityManager em;

	ProposicaoRepositoryCustomImpl(EntityManager em) {
		this.em = em;
	}

	@Override
	public List<SituacaoContagem> contarPorSituacao(Specification<Proposicao> filtro) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<SituacaoContagem> query = cb.createQuery(SituacaoContagem.class);
		Root<Proposicao> root = query.from(Proposicao.class);
		Expression<String> situacao = root.get("situacaoDescricao");

		List<Predicate> condicoes = new ArrayList<>();
		condicoes.add(cb.isNotNull(situacao));
		// unrestricted() devolve predicado nulo: nada a acrescentar.
		Predicate doFiltro = filtro.toPredicate(root, query, cb);
		if (doFiltro != null) {
			condicoes.add(doFiltro);
		}

		query.select(cb.construct(SituacaoContagem.class, situacao, cb.count(root)))
				.where(condicoes.toArray(Predicate[]::new))
				.groupBy(situacao);
		return em.createQuery(query).getResultList();
	}
}
