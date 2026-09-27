package com.example.parlamento.ingestao.mapper;

import com.example.parlamento.domain.entity.Proposicao;
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
}
