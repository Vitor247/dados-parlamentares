package com.example.parlamento.ingestao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Envelope de toda resposta da API da Câmara: {@code { "dados": ..., "links": [...] }}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespostaCamara<T>(T dados, List<Link> links) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Link(String rel, String href) {
	}

	public boolean temProximaPagina() {
		return links != null && links.stream().anyMatch(l -> "next".equals(l.rel()));
	}
}
