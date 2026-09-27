package com.example.parlamento.api.mapper;

import com.example.parlamento.api.dto.PartidoDto;
import com.example.parlamento.api.dto.PartidoResumoDto;
import com.example.parlamento.domain.entity.Partido;
import org.springframework.stereotype.Component;

@Component
public class PartidoApiMapper {

	private final FonteMapper fonteMapper;

	public PartidoApiMapper(FonteMapper fonteMapper) {
		this.fonteMapper = fonteMapper;
	}

	public PartidoDto paraDto(Partido partido) {
		return new PartidoDto(
				partido.getId(),
				partido.getSigla(),
				partido.getNome(),
				fonteMapper.camara("partidos", partido.getId(), partido.getUri(), partido.getAtualizadoEm()));
	}

	/** Null-safe: deputado sem partido resolvido gera {@code "partido": null}. */
	public PartidoResumoDto paraResumo(Partido partido) {
		if (partido == null) {
			return null;
		}
		return new PartidoResumoDto(partido.getId(), partido.getSigla(), partido.getNome());
	}
}
