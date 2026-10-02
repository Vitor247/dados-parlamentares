package com.example.parlamento.api.controller;

import com.example.parlamento.api.dto.DeputadoDto;
import com.example.parlamento.api.dto.DeputadoResumoDto;
import com.example.parlamento.api.dto.PaginaDto;
import com.example.parlamento.api.dto.ProposicaoResumoDto;
import com.example.parlamento.api.dto.SituacoesDto;
import com.example.parlamento.api.service.DeputadoConsultaService;
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

@Tag(name = "Deputados")
@RestController
@RequestMapping("/api/v1/deputados")
@ApiResponse(responseCode = "400", description = "Parâmetro inválido (tipo errado, ordenação por campo inexistente)",
		content = @Content(schema = @Schema(implementation = StandardError.class)))
public class DeputadoController {

	private final DeputadoConsultaService service;

	public DeputadoController(DeputadoConsultaService service) {
		this.service = service;
	}

	@Operation(summary = "Lista deputados",
			description = "Deputados da legislatura importada, ordenados por nome. Filtros opcionais e combináveis.")
	@ApiResponse(responseCode = "200", description = "Página de deputados")
	@GetMapping
	public PaginaDto<DeputadoResumoDto> listar(
			@Parameter(description = "Sigla da UF", example = "MG") @RequestParam(required = false) String uf,
			@Parameter(description = "Sigla do partido atual, resolvida internamente para o partido", example = "PT")
			@RequestParam(required = false) String partido,
			@Parameter(description = "Trecho do nome parlamentar, sem diferenciar maiúsculas", example = "silva")
			@RequestParam(required = false) String nome,
			@ParameterObject @PageableDefault(size = Paginacao.TAMANHO_PADRAO, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
		return service.listar(uf, partido, nome, pageable);
	}

	@Operation(summary = "Detalha um deputado",
			description = "Inclui dados pessoais publicados pela fonte e `totalProposicoes` — contagem do que está "
					+ "na base, dentro do recorte importado; não é métrica de produtividade.")
	@ApiResponse(responseCode = "200", description = "Deputado encontrado")
	@ApiResponse(responseCode = "404", description = "Deputado não encontrado",
			content = @Content(schema = @Schema(implementation = StandardError.class)))
	@GetMapping("/{id}")
	public DeputadoDto buscar(@Parameter(description = "Id oficial do deputado na Câmara", example = "204379")
			@PathVariable Long id) {
		return service.buscar(id);
	}

	@Operation(summary = "Lista as proposições de autoria do deputado",
			description = "Proposições em que o deputado figura como autor, da mais recente para a mais antiga.")
	@ApiResponse(responseCode = "200", description = "Página de proposições")
	@ApiResponse(responseCode = "404", description = "Deputado não encontrado",
			content = @Content(schema = @Schema(implementation = StandardError.class)))
	@GetMapping("/{id}/proposicoes")
	public PaginaDto<ProposicaoResumoDto> listarProposicoes(
			@Parameter(description = "Id oficial do deputado na Câmara", example = "204379") @PathVariable Long id,
			@Parameter(description = "Ano da proposição", example = "2025") @RequestParam(required = false) Integer ano,
			@Parameter(description = "Sigla do tipo (PL, PEC, PLP, PDL)", example = "PL") @RequestParam(required = false) String tipo,
			@Parameter(description = "Situação atual, pelo texto exato (valores em /proposicoes/situacoes)",
					example = "Aguardando Parecer") @RequestParam(required = false) String situacao,
			@ParameterObject @PageableDefault(size = Paginacao.TAMANHO_PADRAO, sort = {"ano", "numero"}, direction = Sort.Direction.DESC) Pageable pageable) {
		return service.listarProposicoes(id, ano, tipo, situacao, pageable);
	}

	@Operation(summary = "Lista as situações das proposições do deputado",
			description = "Situações em que o deputado tem proposições de autoria, com os totais, respeitando os "
					+ "filtros `ano` e `tipo`: só aparecem opções que retornam resultado.")
	@ApiResponse(responseCode = "200", description = "Situações e totais")
	@ApiResponse(responseCode = "404", description = "Deputado não encontrado",
			content = @Content(schema = @Schema(implementation = StandardError.class)))
	@GetMapping("/{id}/proposicoes/situacoes")
	public SituacoesDto listarSituacoesDasProposicoes(
			@Parameter(description = "Id oficial do deputado na Câmara", example = "204379") @PathVariable Long id,
			@Parameter(description = "Ano da proposição", example = "2025") @RequestParam(required = false) Integer ano,
			@Parameter(description = "Sigla do tipo (PL, PEC, PLP, PDL)", example = "PL") @RequestParam(required = false) String tipo) {
		return service.listarSituacoesDasProposicoes(id, ano, tipo);
	}
}
