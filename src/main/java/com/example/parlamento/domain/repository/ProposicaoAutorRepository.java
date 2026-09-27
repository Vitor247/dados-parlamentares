package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.ProposicaoAutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Set;

public interface ProposicaoAutorRepository extends JpaRepository<ProposicaoAutor, Long> {

	/** Das proposições informadas, quais já têm o deputado como autor. */
	@Query("""
			select a.proposicao.id from ProposicaoAutor a
			where a.deputado.id = :deputadoId and a.proposicao.id in :proposicaoIds""")
	Set<Long> findProposicaoIdsComAutor(Long deputadoId, Collection<Long> proposicaoIds);
}
