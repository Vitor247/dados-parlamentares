package com.example.parlamento.ingestao.mapper;

import com.example.parlamento.domain.entity.Partido;
import com.example.parlamento.ingestao.dto.PartidoCamaraDto;

import java.time.LocalDateTime;

/** DTO externo de partido → entidade. Atualiza no lugar para permitir upsert por id. */
public final class PartidoMapper {

	private PartidoMapper() {
	}

	public static void atualizar(Partido partido, PartidoCamaraDto dto, LocalDateTime agora) {
		partido.setSigla(dto.sigla());
		partido.setNome(dto.nome());
		partido.setUri(dto.uri());
		partido.setAtualizadoEm(agora);
	}
}
