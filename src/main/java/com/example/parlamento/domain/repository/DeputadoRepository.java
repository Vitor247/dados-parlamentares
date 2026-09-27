package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.Deputado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeputadoRepository extends JpaRepository<Deputado, Long> {
}
