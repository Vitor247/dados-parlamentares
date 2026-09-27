package com.example.parlamento.api.mapper;

import com.example.parlamento.api.dto.FonteDto;
import com.example.parlamento.config.CamaraProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Monta o {@code _fonte} de cada recurso apontando para a URI oficial na Câmara. */
@Component
public class FonteMapper {

	private final String baseUrl;

	public FonteMapper(CamaraProperties props) {
		this.baseUrl = props.baseUrl().replaceAll("/+$", "");
	}

	/**
	 * @param uriOficial URI gravada na ingestão; se ausente, é montada a partir do recurso e id
	 */
	public FonteDto camara(String recurso, Long id, String uriOficial, LocalDateTime atualizadoEm) {
		String uri = uriOficial != null ? uriOficial : baseUrl + "/" + recurso + "/" + id;
		return new FonteDto(FonteDto.ORIGEM_CAMARA, uri, atualizadoEm);
	}
}
