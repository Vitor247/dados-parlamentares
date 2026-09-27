package com.example.parlamento.api.mapper;

import com.example.parlamento.api.dto.DeputadoDto;
import com.example.parlamento.api.dto.DeputadoResumoDto;
import com.example.parlamento.api.dto.FonteDto;
import com.example.parlamento.domain.entity.Deputado;
import org.springframework.stereotype.Component;

@Component
public class DeputadoApiMapper {

	private final FonteMapper fonteMapper;
	private final PartidoApiMapper partidoMapper;

	public DeputadoApiMapper(FonteMapper fonteMapper, PartidoApiMapper partidoMapper) {
		this.fonteMapper = fonteMapper;
		this.partidoMapper = partidoMapper;
	}

	public DeputadoResumoDto paraResumo(Deputado deputado) {
		return new DeputadoResumoDto(
				deputado.getId(),
				deputado.getNome(),
				deputado.getSiglaUf(),
				deputado.getSiglaPartido(),
				partidoMapper.paraResumo(deputado.getPartido()),
				deputado.getSituacao(),
				deputado.getUrlFoto(),
				fonte(deputado));
	}

	public DeputadoDto paraDto(Deputado deputado, long totalProposicoes) {
		return new DeputadoDto(
				deputado.getId(),
				deputado.getNome(),
				deputado.getNomeCivil(),
				deputado.getSiglaUf(),
				deputado.getIdLegislatura(),
				deputado.getSiglaPartido(),
				partidoMapper.paraResumo(deputado.getPartido()),
				deputado.getSituacao(),
				deputado.getCondicaoEleitoral(),
				deputado.getEmail(),
				deputado.getUrlFoto(),
				deputado.getDataNascimento(),
				deputado.getUfNascimento(),
				deputado.getMunicipioNascimento(),
				deputado.getEscolaridade(),
				totalProposicoes,
				fonte(deputado));
	}

	private FonteDto fonte(Deputado deputado) {
		return fonteMapper.camara("deputados", deputado.getId(), null, deputado.getAtualizadoEm());
	}
}
