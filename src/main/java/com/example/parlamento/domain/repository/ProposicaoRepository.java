package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Proposicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProposicaoRepository extends JpaRepository<Proposicao, Long>, JpaSpecificationExecutor<Proposicao> {
}
