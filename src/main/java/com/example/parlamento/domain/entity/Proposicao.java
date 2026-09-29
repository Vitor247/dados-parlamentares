package com.example.parlamento.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Proposição legislativa. A lista da fonte traz só identificação e ementa;
 * situação, data de apresentação e inteiro teor chegam no enriquecimento
 * ({@code detalheCarregado = true}). O status atual é achatado em colunas.
 */
@Entity
@Table(name = "proposicao")
public class Proposicao {

	@Id
	private Long id;

	@Column(name = "sigla_tipo", nullable = false, length = 20)
	private String siglaTipo;

	@Column(name = "cod_tipo")
	private Integer codTipo;

	@Column(name = "descricao_tipo")
	private String descricaoTipo;

	@Column(nullable = false)
	private Integer numero;

	@Column(nullable = false)
	private Integer ano;

	@Column(columnDefinition = "text")
	private String ementa;

	@Column(name = "data_apresentacao")
	private LocalDateTime dataApresentacao;

	@Column(name = "url_inteiro_teor", length = 500)
	private String urlInteiroTeor;

	@Column(name = "situacao_descricao")
	private String situacaoDescricao;

	@Column(name = "situacao_cod")
	private Integer situacaoCod;

	@Column(name = "situacao_data")
	private LocalDateTime situacaoData;

	@Column(name = "situacao_orgao_sigla", length = 50)
	private String situacaoOrgaoSigla;

	@Column(name = "tramitacao_descricao")
	private String tramitacaoDescricao;

	@Column(name = "detalhe_carregado", nullable = false)
	private boolean detalheCarregado;

	/** Quando situação/autoria foram buscadas na fonte (Fase B). Distinto de atualizadoEm. */
	@Column(name = "detalhe_atualizado_em")
	private LocalDateTime detalheAtualizadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private LocalDateTime atualizadoEm;

	protected Proposicao() {
	}

	public Proposicao(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public String getSiglaTipo() {
		return siglaTipo;
	}

	public void setSiglaTipo(String siglaTipo) {
		this.siglaTipo = siglaTipo;
	}

	public Integer getCodTipo() {
		return codTipo;
	}

	public void setCodTipo(Integer codTipo) {
		this.codTipo = codTipo;
	}

	public String getDescricaoTipo() {
		return descricaoTipo;
	}

	public void setDescricaoTipo(String descricaoTipo) {
		this.descricaoTipo = descricaoTipo;
	}

	public Integer getNumero() {
		return numero;
	}

	public void setNumero(Integer numero) {
		this.numero = numero;
	}

	public Integer getAno() {
		return ano;
	}

	public void setAno(Integer ano) {
		this.ano = ano;
	}

	public String getEmenta() {
		return ementa;
	}

	public void setEmenta(String ementa) {
		this.ementa = ementa;
	}

	public LocalDateTime getDataApresentacao() {
		return dataApresentacao;
	}

	public void setDataApresentacao(LocalDateTime dataApresentacao) {
		this.dataApresentacao = dataApresentacao;
	}

	public String getUrlInteiroTeor() {
		return urlInteiroTeor;
	}

	public void setUrlInteiroTeor(String urlInteiroTeor) {
		this.urlInteiroTeor = urlInteiroTeor;
	}

	public String getSituacaoDescricao() {
		return situacaoDescricao;
	}

	public void setSituacaoDescricao(String situacaoDescricao) {
		this.situacaoDescricao = situacaoDescricao;
	}

	public Integer getSituacaoCod() {
		return situacaoCod;
	}

	public void setSituacaoCod(Integer situacaoCod) {
		this.situacaoCod = situacaoCod;
	}

	public LocalDateTime getSituacaoData() {
		return situacaoData;
	}

	public void setSituacaoData(LocalDateTime situacaoData) {
		this.situacaoData = situacaoData;
	}

	public String getSituacaoOrgaoSigla() {
		return situacaoOrgaoSigla;
	}

	public void setSituacaoOrgaoSigla(String situacaoOrgaoSigla) {
		this.situacaoOrgaoSigla = situacaoOrgaoSigla;
	}

	public String getTramitacaoDescricao() {
		return tramitacaoDescricao;
	}

	public void setTramitacaoDescricao(String tramitacaoDescricao) {
		this.tramitacaoDescricao = tramitacaoDescricao;
	}

	public boolean isDetalheCarregado() {
		return detalheCarregado;
	}

	public void setDetalheCarregado(boolean detalheCarregado) {
		this.detalheCarregado = detalheCarregado;
	}

	public LocalDateTime getDetalheAtualizadoEm() {
		return detalheAtualizadoEm;
	}

	public void setDetalheAtualizadoEm(LocalDateTime detalheAtualizadoEm) {
		this.detalheAtualizadoEm = detalheAtualizadoEm;
	}

	public LocalDateTime getAtualizadoEm() {
		return atualizadoEm;
	}

	public void setAtualizadoEm(LocalDateTime atualizadoEm) {
		this.atualizadoEm = atualizadoEm;
	}
}
