package com.example.parlamento.api.dto;

import java.time.LocalDateTime;

/** Procedência de um registro: de onde veio e quando foi atualizado na nossa base. */
public record FonteDto(String origem, String uri, LocalDateTime atualizadoEm) {

	public static final String ORIGEM_CAMARA = "Câmara dos Deputados - Dados Abertos";
}
