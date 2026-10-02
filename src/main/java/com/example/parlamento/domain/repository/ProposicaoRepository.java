package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Proposicao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ProposicaoRepository extends JpaRepository<Proposicao, Long>, JpaSpecificationExecutor<Proposicao>,
		ProposicaoRepositoryCustom {

	/** Nunca enriquecidas (usa o índice parcial idx_proposicao_pendente). */
	@Query("select p.id from Proposicao p where p.detalheCarregado = false order by p.id")
	List<Long> findIdsPendentes(Pageable lote);

	long countByDetalheCarregadoFalse();

	/** Enriquecidas antes do corte, da situação mais antiga para a mais recente. */
	@Query("""
			select p.id from Proposicao p
			where p.detalheCarregado = true and p.detalheAtualizadoEm < :corte
			order by p.detalheAtualizadoEm, p.id""")
	List<Long> findIdsDesatualizados(LocalDateTime corte, Pageable lote);

	@Query("select count(p) from Proposicao p where p.detalheCarregado = true and p.detalheAtualizadoEm < :corte")
	long countDesatualizadas(LocalDateTime corte);
}
