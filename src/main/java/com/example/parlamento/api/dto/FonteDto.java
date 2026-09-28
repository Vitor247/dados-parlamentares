package com.example.parlamento.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Procedência de um registro: de onde veio e quando foi atualizado na nossa base. */
@Schema(description = "Procedência do dado")
public record FonteDto(
		@Schema(example = "Câmara dos Deputados - Dados Abertos") String origem,
		@Schema(description = "URI oficial do recurso na API da Câmara",
				example = "https://dadosabertos.camara.leg.br/api/v2/deputados/204379") String uri,
		@Schema(description = "Última atualização deste registro na nossa base") LocalDateTime atualizadoEm) {

	public static final String ORIGEM_CAMARA = "Câmara dos Deputados - Dados Abertos";
}
