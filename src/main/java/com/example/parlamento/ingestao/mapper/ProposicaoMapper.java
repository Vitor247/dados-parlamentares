package com.example.parlamento.ingestao.mapper;

import com.example.parlamento.domain.entity.Proposicao;
import com.example.parlamento.ingestao.dto.ProposicaoDetalheCamaraDto;
import com.example.parlamento.ingestao.dto.ProposicaoResumoCamaraDto;

import java.time.LocalDateTime;

/** DTOs externos de proposição → entidade. */
public final class ProposicaoMapper {

	private ProposicaoMapper() {
	}

	/**
	 * Campos da lista (identificação e ementa). Não mexe em {@code detalheCarregado}
	 * nem nos campos do enriquecimento: rodar a Fase A de novo não desfaz a Fase B.
	 */
	public static void atualizarDoResumo(Proposicao proposicao, ProposicaoResumoCamaraDto dto, LocalDateTime agora) {
		proposicao.setSiglaTipo(dto.siglaTipo());
		proposicao.setCodTipo(dto.codTipo());
		proposicao.setNumero(dto.numero());
		proposicao.setAno(dto.ano());
		proposicao.setEmenta(dto.ementa());
		proposicao.setAtualizadoEm(agora);
	}

	/** Enriquecimento (Fase B): detalhe completo + situação atual achatada em colunas. */
	public static void atualizarDoDetalhe(Proposicao proposicao, ProposicaoDetalheCamaraDto dto, LocalDateTime agora) {
		proposicao.setSiglaTipo(dto.siglaTipo());
		proposicao.setCodTipo(dto.codTipo());
		proposicao.setDescricaoTipo(dto.descricaoTipo());
		proposicao.setNumero(dto.numero());
		proposicao.setAno(dto.ano());
		proposicao.setEmenta(dto.ementa());
		proposicao.setDataApresentacao(DatasCamara.dataHora(dto.dataApresentacao()));
		proposicao.setUrlInteiroTeor(dto.urlInteiroTeor());

		ProposicaoDetalheCamaraDto.StatusProposicao status = dto.statusProposicao();
		proposicao.setSituacaoDescricao(status == null ? null : status.descricaoSituacao());
		proposicao.setSituacaoCod(status == null ? null : status.codSituacao());
		proposicao.setSituacaoData(status == null ? null : DatasCamara.dataHora(status.dataHora()));
		proposicao.setSituacaoOrgaoSigla(status == null ? null : status.siglaOrgao());
		proposicao.setTramitacaoDescricao(status == null ? null : status.descricaoTramitacao());

		proposicao.setDetalheCarregado(true);
		proposicao.setAtualizadoEm(agora);
	}
}
