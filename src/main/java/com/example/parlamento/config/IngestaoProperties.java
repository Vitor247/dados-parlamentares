package com.example.parlamento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;
import java.util.List;

/**
 * Recorte de dados da ingestão ({@code parlamento.ingestao.*}).
 * Ampliar o recorte é só mudar a configuração.
 *
 * @param legislatura            legislatura importada (partidos e deputados)
 * @param dataApresentacaoInicio proposições apresentadas a partir desta data
 * @param tiposProposicao        siglas de tipo importadas (PL, PEC...)
 */
@ConfigurationProperties(prefix = "parlamento.ingestao")
public record IngestaoProperties(
		int legislatura,
		LocalDate dataApresentacaoInicio,
		List<String> tiposProposicao) {
}
