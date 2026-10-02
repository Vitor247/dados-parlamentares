package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Proposicao;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/** Consultas de proposição que as Specifications sozinhas não expressam (agregação). */
public interface ProposicaoRepositoryCustom {

	/**
	 * Situações das proposições que atendem ao filtro, agrupadas pela descrição (códigos
	 * distintos podem compartilhar o texto), com o total em cada uma. Ignora as sem situação.
	 */
	List<SituacaoContagem> contarPorSituacao(Specification<Proposicao> filtro);
}
