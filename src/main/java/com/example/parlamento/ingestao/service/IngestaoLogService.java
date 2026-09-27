package com.example.parlamento.ingestao.service;

import com.example.parlamento.domain.entity.IngestaoLog;
import com.example.parlamento.domain.repository.IngestaoLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Grava linhas em {@code ingestao_log}. Cada registro roda em transação própria
 * (REQUIRES_NEW) para sobreviver ao rollback da unidade que falhou.
 */
@Service
public class IngestaoLogService {

	private final IngestaoLogRepository repository;

	public IngestaoLogService(IngestaoLogRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sucesso(String recurso, String referencia, int registros, LocalDateTime iniciadoEm) {
		registrar(recurso, referencia, IngestaoLog.Status.SUCESSO, registros, null, iniciadoEm);
	}

	/** Gravou o essencial, mas parte do dado não pôde ser obtida ou resolvida. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void parcial(String recurso, String referencia, int registros, String motivo, LocalDateTime iniciadoEm) {
		registrar(recurso, referencia, IngestaoLog.Status.PARCIAL, registros, motivo, iniciadoEm);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void falha(String recurso, String referencia, Throwable erro, LocalDateTime iniciadoEm) {
		registrar(recurso, referencia, IngestaoLog.Status.FALHA, null, descrever(erro), iniciadoEm);
	}

	private void registrar(String recurso, String referencia, IngestaoLog.Status status,
			Integer registros, String mensagemErro, LocalDateTime iniciadoEm) {
		IngestaoLog log = new IngestaoLog(recurso, referencia, iniciadoEm);
		log.setStatus(status);
		log.setRegistros(registros);
		log.setMensagemErro(mensagemErro);
		log.setFinalizadoEm(LocalDateTime.now());
		repository.save(log);
	}

	static String descrever(Throwable erro) {
		StringBuilder sb = new StringBuilder(erro.getClass().getSimpleName()).append(": ").append(erro.getMessage());
		for (Throwable causa = erro.getCause(); causa != null && causa != erro; causa = causa.getCause()) {
			sb.append(" | causa: ").append(causa.getClass().getSimpleName()).append(": ").append(causa.getMessage());
		}
		return sb.toString();
	}
}
