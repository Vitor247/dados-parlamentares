package com.example.parlamento.domain.repository;

import com.example.parlamento.domain.entity.IngestaoLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestaoLogRepository extends JpaRepository<IngestaoLog, Long> {
}
