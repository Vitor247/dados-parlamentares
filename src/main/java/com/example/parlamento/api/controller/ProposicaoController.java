package com.example.parlamento.api.controller;

import com.example.parlamento.api.dto.AutoresDto;
import com.example.parlamento.api.dto.PaginaDto;
import com.example.parlamento.api.dto.ProposicaoDto;
import com.example.parlamento.api.dto.ProposicaoResumoDto;
import com.example.parlamento.api.service.ProposicaoConsultaService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/proposicoes")
public class ProposicaoController {

	private final ProposicaoConsultaService service;

	public ProposicaoController(ProposicaoConsultaService service) {
		this.service = service;
	}

	/** Ex.: {@code ?tipo=PL&numero=1234&ano=2025} localiza "PL 1234/2025". */
	@GetMapping
	public PaginaDto<ProposicaoResumoDto> listar(
			@RequestParam(required = false) Integer ano,
			@RequestParam(required = false) String tipo,
			@RequestParam(required = false) Integer numero,
			@RequestParam(required = false) String ementa,
			@PageableDefault(size = Paginacao.TAMANHO_PADRAO, sort = {"ano", "numero"}, direction = Sort.Direction.DESC) Pageable pageable) {
		return service.listar(ano, tipo, numero, ementa, pageable);
	}

	@GetMapping("/{id}")
	public ProposicaoDto buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@GetMapping("/{id}/autores")
	public AutoresDto listarAutores(@PathVariable Long id) {
		return service.listarAutores(id);
	}
}
