package com.example.parlamento.api.controller;

import com.example.parlamento.config.OpenApiConfig;
import com.example.parlamento.exception.RecursoNaoEncontradoException;
import com.example.parlamento.exception.StandardError;
import com.example.parlamento.ingestao.service.ExecucaoCargaBaseService;
import com.example.parlamento.ingestao.service.IngestaoService;
import com.example.parlamento.ingestao.service.ResultadoIngestao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** Disparo manual da ingestão. Protegido por chave (ver AdminApiKeyInterceptor). */
@Tag(name = "Administração")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_ADMIN)
@RestController
@RequestMapping("/api/v1/admin/ingestao")
@ApiResponse(responseCode = "401", description = "Header X-Admin-Key ausente ou inválido",
		content = @Content(schema = @Schema(implementation = StandardError.class)))
@ApiResponse(responseCode = "403", description = "Administração desabilitada: servidor sem ADMIN_API_KEY",
		content = @Content(schema = @Schema(implementation = StandardError.class)))
public class AdminIngestaoController {

	private final IngestaoService ingestaoService;
	private final ExecucaoCargaBaseService execucoes;

	public AdminIngestaoController(IngestaoService ingestaoService, ExecucaoCargaBaseService execucoes) {
		this.ingestaoService = ingestaoService;
		this.execucoes = execucoes;
	}

	@Operation(summary = "Dispara a carga base (Fase A) em segundo plano",
			description = "Assíncrono: responde na hora com o id da execução e importa, em segundo plano, partidos → "
					+ "deputados → proposições por autoria, dentro do recorte configurado. Acompanhe por "
					+ "`GET /execucoes/{id}` (também no header `Location`). Idempotente. Uma carga por vez.")
	@ApiResponse(responseCode = "202", description = "Carga iniciada")
	@ApiResponse(responseCode = "409", description = "Já existe uma carga base em andamento",
			content = @Content(schema = @Schema(implementation = StandardError.class)))
	@PostMapping("/base")
	public ResponseEntity<ExecucaoCargaBaseService.Execucao> cargaBase() {
		ExecucaoCargaBaseService.Execucao execucao = execucoes.iniciar();
		return ResponseEntity.accepted()
				.location(URI.create("/api/v1/admin/ingestao/execucoes/" + execucao.id()))
				.body(execucao);
	}

	@Operation(summary = "Consulta uma execução da carga base",
			description = "Status `EM_ANDAMENTO`, `CONCLUIDA` (com o resultado por etapa) ou `FALHOU` (com o erro). "
					+ "As execuções ficam em memória: depois de um reinício da API, a consulta responde 404.")
	@ApiResponse(responseCode = "200", description = "Situação da execução")
	@ApiResponse(responseCode = "404", description = "Execução não encontrada",
			content = @Content(schema = @Schema(implementation = StandardError.class)))
	@GetMapping("/execucoes/{id}")
	public ExecucaoCargaBaseService.Execucao execucao(@PathVariable UUID id) {
		return execucoes.buscar(id).orElseThrow(() -> new RecursoNaoEncontradoException("Execução", id));
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
