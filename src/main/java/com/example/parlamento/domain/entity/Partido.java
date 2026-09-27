package com.example.parlamento.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Partido político conforme publicado pela Câmara.
 * A PK é o id oficial: siglas se repetem entre partidos diferentes ao longo do tempo.
 */
@Entity
@Table(name = "partido")
public class Partido {

	@Id
	private Long id;

	@Column(nullable = false, length = 50)
	private String sigla;

	@Column(nullable = false)
	private String nome;

	private String uri;

	@Column(name = "atualizado_em", nullable = false)
	private LocalDateTime atualizadoEm;

	protected Partido() {
	}

	public Partido(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public String getSigla() {
		return sigla;
	}

	public void setSigla(String sigla) {
		this.sigla = sigla;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public LocalDateTime getAtualizadoEm() {
		return atualizadoEm;
	}

	public void setAtualizadoEm(LocalDateTime atualizadoEm) {
		this.atualizadoEm = atualizadoEm;
	}
}
