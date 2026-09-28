package com.example.parlamento.api.controller;

import com.example.parlamento.api.dto.AutoresDto;
import com.example.parlamento.api.dto.PaginaDto;
import com.example.parlamento.api.dto.ProposicaoDto;
import com.example.parlamento.api.dto.ProposicaoResumoDto;
import com.example.parlamento.api.service.ProposicaoConsultaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Proposições")
@RestController
@RequestMapping("/api/v1/proposicoes")
@ApiResponse(responseCode = "400", description = "Parâmetro inválido (tipo errado, ordenação por campo inexistente)",
		content = @Content(schema = @Schema(implementation = StandardError.class)))
public class ProposicaoController {

	private final ProposicaoConsultaService service;

	public ProposicaoController(ProposicaoConsultaService service) {
		this.service = service;
	}

	@Operation(summary = "Busca proposições",
			description = "Filtros opcionais e combináveis. Para localizar \"PL 1234/2025\", use "
					+ "`tipo=PL&numero=1234&ano=2025`. Ordenação padrão: da mais recente para a mais antiga.")
	@ApiResponse(responseCode = "200", description = "Página de proposições")
	@GetMapping
	public PaginaDto<ProposicaoResumoDto> listar(
			@Parameter(description = "Ano da proposição", example = "2025") @RequestParam(required = false) Integer ano,
			@Parameter(description = "Sigla do tipo (PL, PEC, PLP, PDL)", example = "PL") @RequestParam(required = false) String tipo,
			@Parameter(description = "Número da proposição", example = "387") @RequestParam(required = false) Integer numero,
			@Parameter(description = "Trecho da ementa, sem diferenciar maiúsculas", example = "saúde")
			@RequestParam(required = false) String ementa,
			@ParameterObject @PageableDefault(size = Paginacao.TAMANHO_PADRAO, sort = {"ano", "numero"}, direction = Sort.Direction.DESC) Pageable pageable) {
		return service.listar(ano, tipo, numero, ementa, pageable);
	}

	@Operation(summary = "Detalha uma proposição",
			description = "Enquanto `detalheCarregado = false`, situação, data de apresentação e inteiro teor "
					+ "ainda não foram carregados da fonte e vêm como `null`.")
	@ApiResponse(responseCode = "200", description = "Proposição encontrada")
	@ApiResponse(responseCode = "404", description = "Proposição não encontrada",
			content = @Content(schema = @Schema(implementation = StandardError.class)))
	@GetMapping("/{id}")
	public ProposicaoDto buscar(@Parameter(description = "Id oficial da proposição na Câmara", example = "2483650")
			@PathVariable Long id) {
		return service.buscar(id);
	}

	@Operation(summary = "Lista os autores da proposição",
			description = "Na ordem de assinatura. O autor pode não ser deputado (Poder Executivo, Senado, comissão): "
					+ "nesse caso `deputado` é `null`. Enquanto `detalheCarregado = false`, a lista tem apenas os "
					+ "deputados da base, sem coautores externos, ordem ou proponente.")
	@ApiResponse(responseCode = "200", description = "Autoria da proposição")
	@ApiResponse(responseCode = "404", description = "Proposição não encontrada",
			content = @Content(schema = @Schema(implementation = StandardError.class)))
	@GetMapping("/{id}/autores")
	public AutoresDto listarAutores(@Parameter(description = "Id oficial da proposição na Câmara", example = "2483650")
			@PathVariable Long id) {
		return service.listarAutores(id);
	}
}
