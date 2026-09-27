package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Partido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartidoRepository extends JpaRepository<Partido, Long> {
}
