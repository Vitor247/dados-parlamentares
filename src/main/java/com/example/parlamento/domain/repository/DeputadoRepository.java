package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Deputado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeputadoRepository extends JpaRepository<Deputado, Long>, JpaSpecificationExecutor<Deputado> {

	/** Traz o partido no mesmo SELECT para evitar N+1 nas listagens filtradas. */
	@Override
	@EntityGraph(attributePaths = "partido")
	Page<Deputado> findAll(Specification<Deputado> spec, Pageable pageable);

	@EntityGraph(attributePaths = "partido")
	Optional<Deputado> findComPartidoById(Long id);

	@EntityGraph(attributePaths = "partido")
	Page<Deputado> findByPartidoId(Long partidoId, Pageable pageable);

	@Query("select d.id from Deputado d order by d.id")
	List<Long> findAllIds();
}
