package com.example.parlamento.ingestao.service;

import com.example.parlamento.config.IngestaoProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Orquestra as fases de ingestão na ordem exigida pelas dependências entre recursos. */
@Service
public class IngestaoService {

	private final IngestaoProperties props;
	private final PartidoIngestaoService partidoIngestao;

	public IngestaoService(IngestaoProperties props, PartidoIngestaoService partidoIngestao) {
		this.props = props;
		this.partidoIngestao = partidoIngestao;
	}

	/** Fase A — carga base. */
	public ResultadoIngestao executarCargaBase() {
		LocalDateTime inicio = LocalDateTime.now();
		List<ResultadoIngestao.Etapa> etapas = new ArrayList<>();

		etapas.add(partidoIngestao.importar(props.legislatura()));

		return new ResultadoIngestao(inicio, LocalDateTime.now(), etapas);
	}
}
