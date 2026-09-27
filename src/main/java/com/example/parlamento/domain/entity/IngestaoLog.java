package com.example.parlamento.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Registro de cada unidade de ingestão (um deputado, uma proposição, um recurso inteiro). */
@Entity
@Table(name = "ingestao_log")
public class IngestaoLog {

	public enum Status { SUCESSO, FALHA, PARCIAL }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** partidos | deputados | proposicoes | enriquecimento */
	@Column(nullable = false, length = 50)
	private String recurso;

	@Column(length = 100)
	private String referencia;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	private Integer registros;

	@Column(name = "mensagem_erro", columnDefinition = "text")
	private String mensagemErro;

	@Column(name = "iniciado_em", nullable = false)
	private LocalDateTime iniciadoEm;

	@Column(name = "finalizado_em")
	private LocalDateTime finalizadoEm;

	protected IngestaoLog() {
	}

	public IngestaoLog(String recurso, String referencia, LocalDateTime iniciadoEm) {
		this.recurso = recurso;
		this.referencia = referencia;
		this.iniciadoEm = iniciadoEm;
	}

	public Long getId() {
		return id;
	}

	public String getRecurso() {
		return recurso;
	}

	public String getReferencia() {
		return referencia;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Integer getRegistros() {
		return registros;
	}

	public void setRegistros(Integer registros) {
		this.registros = registros;
	}

	public String getMensagemErro() {
		return mensagemErro;
	}

	public void setMensagemErro(String mensagemErro) {
		this.mensagemErro = mensagemErro;
	}

	public LocalDateTime getIniciadoEm() {
		return iniciadoEm;
	}

	public LocalDateTime getFinalizadoEm() {
		return finalizadoEm;
	}

	public void setFinalizadoEm(LocalDateTime finalizadoEm) {
		this.finalizadoEm = finalizadoEm;
	}
}
