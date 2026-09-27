package com.example.parlamento.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Deputado federal. O vínculo com {@link Partido} vem da lista de deputados;
 * o detalhe só enriquece os campos pessoais. CPF não é armazenado.
 */
@Entity
@Table(name = "deputado")
public class Deputado {

	@Id
	private Long id;

	@Column(nullable = false)
	private String nome;

	@Column(name = "nome_civil")
	private String nomeCivil;

	@Column(name = "sigla_uf", length = 2)
	private String siglaUf;

	@Column(name = "id_legislatura")
	private Integer idLegislatura;

	@Column(name = "url_foto", length = 500)
	private String urlFoto;

	private String email;

	@Column(name = "data_nascimento")
	private LocalDate dataNascimento;

	@Column(name = "uf_nascimento", length = 2)
	private String ufNascimento;

	@Column(name = "municipio_nascimento")
	private String municipioNascimento;

	private String escolaridade;

	@Column(length = 100)
	private String situacao;

	@Column(name = "condicao_eleitoral", length = 100)
	private String condicaoEleitoral;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partido_id")
	private Partido partido;

	/** Retrato da sigla informada pela fonte; útil quando o partido não foi resolvido. */
	@Column(name = "sigla_partido", length = 50)
	private String siglaPartido;

	@Column(name = "atualizado_em", nullable = false)
	private LocalDateTime atualizadoEm;

	protected Deputado() {
	}

	public Deputado(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getNomeCivil() {
		return nomeCivil;
	}

	public void setNomeCivil(String nomeCivil) {
		this.nomeCivil = nomeCivil;
	}

	public String getSiglaUf() {
		return siglaUf;
	}

	public void setSiglaUf(String siglaUf) {
		this.siglaUf = siglaUf;
	}

	public Integer getIdLegislatura() {
		return idLegislatura;
	}

	public void setIdLegislatura(Integer idLegislatura) {
		this.idLegislatura = idLegislatura;
	}

	public String getUrlFoto() {
		return urlFoto;
	}

	public void setUrlFoto(String urlFoto) {
		this.urlFoto = urlFoto;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	public String getUfNascimento() {
		return ufNascimento;
	}

	public void setUfNascimento(String ufNascimento) {
		this.ufNascimento = ufNascimento;
	}

	public String getMunicipioNascimento() {
		return municipioNascimento;
	}

	public void setMunicipioNascimento(String municipioNascimento) {
		this.municipioNascimento = municipioNascimento;
	}

	public String getEscolaridade() {
		return escolaridade;
	}

	public void setEscolaridade(String escolaridade) {
		this.escolaridade = escolaridade;
	}

	public String getSituacao() {
		return situacao;
	}

	public void setSituacao(String situacao) {
		this.situacao = situacao;
	}

	public String getCondicaoEleitoral() {
		return condicaoEleitoral;
	}

	public void setCondicaoEleitoral(String condicaoEleitoral) {
		this.condicaoEleitoral = condicaoEleitoral;
	}

	public Partido getPartido() {
		return partido;
	}

	public void setPartido(Partido partido) {
		this.partido = partido;
	}

	public String getSiglaPartido() {
		return siglaPartido;
	}

	public void setSiglaPartido(String siglaPartido) {
		this.siglaPartido = siglaPartido;
	}

	public LocalDateTime getAtualizadoEm() {
		return atualizadoEm;
	}

	public void setAtualizadoEm(LocalDateTime atualizadoEm) {
		this.atualizadoEm = atualizadoEm;
	}
}
