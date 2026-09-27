package com.example.parlamento.api.mapper;

import com.example.parlamento.api.dto.ProposicaoResumoDto;
import com.example.parlamento.domain.entity.Proposicao;
import org.springframework.stereotype.Component;

@Component
public class ProposicaoApiMapper {

	private final FonteMapper fonteMapper;

	public ProposicaoApiMapper(FonteMapper fonteMapper) {
		this.fonteMapper = fonteMapper;
	}

	public ProposicaoResumoDto paraResumo(Proposicao proposicao) {
		return new ProposicaoResumoDto(
				proposicao.getId(),
				identificacao(proposicao),
				proposicao.getSiglaTipo(),
				proposicao.getNumero(),
				proposicao.getAno(),
				proposicao.getEmenta(),
				proposicao.getDataApresentacao(),
				proposicao.getSituacaoDescricao(),
				proposicao.isDetalheCarregado(),
				fonteMapper.camara("proposicoes", proposicao.getId(), null, proposicao.getAtualizadoEm()));
	}

	/** "PL 1234/2025" — o identificador que as pessoas de fato usam. */
	public static String identificacao(Proposicao proposicao) {
		return proposicao.getSiglaTipo() + " " + proposicao.getNumero() + "/" + proposicao.getAno();
	}
}
