package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Proposicao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProposicaoRepository extends JpaRepository<Proposicao, Long>, JpaSpecificationExecutor<Proposicao> {

	/** Próximo lote para o enriquecimento (usa o índice parcial idx_proposicao_pendente). */
	@Query("select p.id from Proposicao p where p.detalheCarregado = false order by p.id")
	List<Long> findIdsPendentes(Pageable lote);

	long countByDetalheCarregadoFalse();
}
