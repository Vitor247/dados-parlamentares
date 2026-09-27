package com.example.parlamento.api.controller;

import com.example.parlamento.api.dto.DeputadoResumoDto;
import com.example.parlamento.api.dto.PaginaDto;
import com.example.parlamento.api.dto.PartidoDto;
import com.example.parlamento.api.service.PartidoConsultaService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidos")
public class PartidoController {

	private final PartidoConsultaService service;

	public PartidoController(PartidoConsultaService service) {
		this.service = service;
	}

	@GetMapping
	public PaginaDto<PartidoDto> listar(@PageableDefault(size = Paginacao.TAMANHO_PADRAO, sort = "sigla", direction = Sort.Direction.ASC) Pageable pageable) {
		return service.listar(pageable);
	}

	@GetMapping("/{id}")
	public PartidoDto buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@GetMapping("/{id}/deputados")
	public PaginaDto<DeputadoResumoDto> listarDeputados(
			@PathVariable Long id,
			@PageableDefault(size = Paginacao.TAMANHO_PADRAO, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
		return service.listarDeputados(id, pageable);
	}
}
