package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Partido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface PartidoRepository extends JpaRepository<Partido, Long> {

	@Query("select p.id from Partido p")
	Set<Long> findAllIds();
}
