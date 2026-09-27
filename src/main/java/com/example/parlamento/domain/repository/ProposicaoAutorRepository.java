package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.ProposicaoAutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Set;

public interface ProposicaoAutorRepository extends JpaRepository<ProposicaoAutor, Long> {

	/** Das proposições informadas, quais já têm o deputado como autor. */
	@Query("""
			select a.proposicao.id from ProposicaoAutor a
			where a.deputado.id = :deputadoId and a.proposicao.id in :proposicaoIds""")
	Set<Long> findProposicaoIdsComAutor(Long deputadoId, Collection<Long> proposicaoIds);

	/** Proposições na base em que o deputado figura como autor. */
	long countByDeputadoId(Long deputadoId);

	/**
	 * DELETE em massa, executado na hora. Necessário porque o Hibernate faz os INSERTs
	 * antes dos DELETEs no flush, o que violaria uk_autor_deputado ao regravar autores.
	 */
	@Modifying
	@Query("delete from ProposicaoAutor a where a.proposicao.id = :proposicaoId")
	int deleteByProposicaoId(Long proposicaoId);
}
