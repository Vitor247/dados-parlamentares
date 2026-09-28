package com.example.parlamento.api.controller;

import com.example.parlamento.exception.StandardError;
import com.example.parlamento.ingestao.service.IngestaoService;
import com.example.parlamento.ingestao.service.ResultadoIngestao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Disparo manual e síncrono da ingestão. Sem autenticação no MVP. */
@Tag(name = "Administração")
@RestController
@RequestMapping("/api/v1/admin/ingestao")
public class AdminIngestaoController {

	private final IngestaoService ingestaoService;

	public AdminIngestaoController(IngestaoService ingestaoService) {
		this.ingestaoService = ingestaoService;
	}

	@Operation(summary = "Executa a carga base (Fase A)",
			description = "Síncrono. Importa partidos → deputados → proposições por autoria, dentro do recorte "
					+ "configurado. Idempotente: pode ser executado de novo sem duplicar dados. Leva alguns minutos.")
	@ApiResponse(responseCode = "200", description = "Resumo da execução, por etapa")
	@ApiResponse(responseCode = "502", description = "API da Câmara indisponível mesmo após as retentativas",
			content = @Content(schema = @Schema(implementation = StandardError.class)))
	@PostMapping("/base")
	public ResultadoIngestao cargaBase() {
		return ingestaoService.executarCargaBase();
	}

	@Operation(summary = "Enriquece proposições pendentes (Fase B)",
			description = "Síncrono. Carrega situação, data de apresentação, inteiro teor e autoria completa de até "
					+ "`limite` proposições com `detalheCarregado = false`. Retomável: o que falhar continua pendente "
					+ "para a próxima execução.")
	@ApiResponse(responseCode = "200", description = "Resumo da execução e quantas proposições ainda estão pendentes")
	@ApiResponse(responseCode = "400", description = "Limite inválido",
			content = @Content(schema = @Schema(implementation = StandardError.class)))
	@PostMapping("/enriquecimento")
	public ResultadoIngestao enriquecimento(
			@Parameter(description = "Máximo de proposições processadas nesta execução", example = "500")
			@RequestParam(defaultValue = "500") @Min(value = 1, message = "deve ser maior que zero") int limite) {
		return ingestaoService.executarEnriquecimento(limite);
	}
}
