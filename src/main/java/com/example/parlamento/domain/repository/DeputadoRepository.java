package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Deputado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeputadoRepository extends JpaRepository<Deputado, Long> {

	/** Traz o partido no mesmo SELECT para evitar N+1 nas listagens. */
	@Override
	@EntityGraph(attributePaths = "partido")
	Page<Deputado> findAll(Pageable pageable);

	@EntityGraph(attributePaths = "partido")
	Optional<Deputado> findComPartidoById(Long id);
}
