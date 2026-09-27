package com.example.parlamento.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Autoria de uma proposição (associação N:N deputado ↔ proposição).
 * O autor nem sempre é deputado (Executivo, Senado, comissão...): nesses casos
 * {@code deputado} fica null, e {@code nome}/{@code tipo} guardam quem é.
 */
@Entity
@Table(name = "proposicao_autor")
public class ProposicaoAutor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "proposicao_id", nullable = false)
	private Proposicao proposicao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "deputado_id")
	private Deputado deputado;

	@Column(nullable = false)
	private String nome;

	@Column(length = 100)
	private String tipo;

	@Column(name = "cod_tipo")
	private Integer codTipo;

	@Column(name = "ordem_assinatura")
	private Integer ordemAssinatura;

	private Integer proponente;

	protected ProposicaoAutor() {
	}

	public ProposicaoAutor(Proposicao proposicao, String nome) {
		this.proposicao = proposicao;
		this.nome = nome;
	}

	public Long getId() {
		return id;
	}

	public Proposicao getProposicao() {
		return proposicao;
	}

	public Deputado getDeputado() {
		return deputado;
	}

	public void setDeputado(Deputado deputado) {
		this.deputado = deputado;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public Integer getCodTipo() {
		return codTipo;
	}

	public void setCodTipo(Integer codTipo) {
		this.codTipo = codTipo;
	}

	public Integer getOrdemAssinatura() {
		return ordemAssinatura;
	}

	public void setOrdemAssinatura(Integer ordemAssinatura) {
		this.ordemAssinatura = ordemAssinatura;
	}

	public Integer getProponente() {
		return proponente;
	}

	public void setProponente(Integer proponente) {
		this.proponente = proponente;
	}
}
