package com.example.parlamento.api.controller;

import com.example.parlamento.ingestao.service.IngestaoService;
import com.example.parlamento.ingestao.service.ResultadoIngestao;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Disparo manual e síncrono da ingestão. Sem autenticação no MVP. */
@RestController
@RequestMapping("/api/v1/admin/ingestao")
public class AdminIngestaoController {

	private final IngestaoService ingestaoService;

	public AdminIngestaoController(IngestaoService ingestaoService) {
		this.ingestaoService = ingestaoService;
	}

	@PostMapping("/base")
	public ResultadoIngestao cargaBase() {
		return ingestaoService.executarCargaBase();
	}
}
