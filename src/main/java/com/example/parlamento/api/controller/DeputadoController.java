package com.example.parlamento.api.controller;

import com.example.parlamento.api.dto.DeputadoDto;
import com.example.parlamento.api.dto.DeputadoResumoDto;
import com.example.parlamento.api.dto.PaginaDto;
import com.example.parlamento.api.dto.ProposicaoResumoDto;
import com.example.parlamento.api.service.DeputadoConsultaService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/deputados")
public class DeputadoController {

	private final DeputadoConsultaService service;

	public DeputadoController(DeputadoConsultaService service) {
		this.service = service;
	}

	/**
	 * @param uf      sigla da UF (MG, SP...)
	 * @param partido sigla do partido (PT, PL...), resolvida internamente para o id
	 * @param nome    trecho do nome parlamentar, sem diferenciar maiúsculas
	 */
	@GetMapping
	public PaginaDto<DeputadoResumoDto> listar(
			@RequestParam(required = false) String uf,
			@RequestParam(required = false) String partido,
			@RequestParam(required = false) String nome,
			@PageableDefault(size = Paginacao.TAMANHO_PADRAO, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
		return service.listar(uf, partido, nome, pageable);
	}

	@GetMapping("/{id}")
	public DeputadoDto buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@GetMapping("/{id}/proposicoes")
	public PaginaDto<ProposicaoResumoDto> listarProposicoes(
			@PathVariable Long id,
			@RequestParam(required = false) Integer ano,
			@RequestParam(required = false) String tipo,
			@PageableDefault(size = Paginacao.TAMANHO_PADRAO, sort = {"ano", "numero"}, direction = Sort.Direction.DESC) Pageable pageable) {
		return service.listarProposicoes(id, ano, tipo, pageable);
	}
}
