package com.example.parlamento.api.controller;

import com.example.parlamento.api.dto.DeputadoResumoDto;
import com.example.parlamento.api.dto.PaginaDto;
import com.example.parlamento.api.dto.PartidoDto;
import com.example.parlamento.api.service.PartidoConsultaService;
import com.example.parlamento.exception.StandardError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Partidos")
@RestController
@RequestMapping("/api/v1/partidos")
@ApiResponse(responseCode = "400", description = "Parâmetro inválido (tipo errado, ordenação por campo inexistente)",
		content = @Content(schema = @Schema(implementation = StandardError.class)))
public class PartidoController {

	private final PartidoConsultaService service;

	public PartidoController(PartidoConsultaService service) {
		this.service = service;
	}

	@Operation(summary = "Lista partidos",
			description = "Partidos com representação na legislatura importada, incluindo extintos ou incorporados. "
					+ "A chave é o id oficial: siglas podem se repetir entre partidos diferentes ao longo do tempo.")
	@ApiResponse(responseCode = "200", description = "Página de partidos")
	@GetMapping
	public PaginaDto<PartidoDto> listar(
			@ParameterObject @PageableDefault(size = Paginacao.TAMANHO_PADRAO, sort = "sigla", direction = Sort.Direction.ASC) Pageable pageable) {
		return service.listar(pageable);
	}

	@Operation(summary = "Detalha um partido")
	@ApiResponse(responseCode = "200", description = "Partido encontrado")
	@ApiResponse(responseCode = "404", description = "Partido não encontrado",
			content = @Content(schema = @Schema(implementation = StandardError.class)))
	@GetMapping("/{id}")
	public PartidoDto buscar(@Parameter(description = "Id oficial do partido na Câmara", example = "36844")
			@PathVariable Long id) {
		return service.buscar(id);
	}

	@Operation(summary = "Lista os deputados do partido",
			description = "Deputados cujo partido atual, segundo a fonte, é este.")
	@ApiResponse(responseCode = "200", description = "Página de deputados")
	@ApiResponse(responseCode = "404", description = "Partido não encontrado",
			content = @Content(schema = @Schema(implementation = StandardError.class)))
	@GetMapping("/{id}/deputados")
	public PaginaDto<DeputadoResumoDto> listarDeputados(
			@Parameter(description = "Id oficial do partido na Câmara", example = "36844") @PathVariable Long id,
			@ParameterObject @PageableDefault(size = Paginacao.TAMANHO_PADRAO, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
		return service.listarDeputados(id, pageable);
	}
}
