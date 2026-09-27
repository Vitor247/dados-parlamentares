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
	private final DeputadoIngestaoService deputadoIngestao;

	public IngestaoService(IngestaoProperties props, PartidoIngestaoService partidoIngestao,
			DeputadoIngestaoService deputadoIngestao) {
		this.props = props;
		this.partidoIngestao = partidoIngestao;
		this.deputadoIngestao = deputadoIngestao;
	}

	/** Fase A — carga base: partidos → deputados. */
	public ResultadoIngestao executarCargaBase() {
		LocalDateTime inicio = LocalDateTime.now();
		List<ResultadoIngestao.Etapa> etapas = new ArrayList<>();

		etapas.add(partidoIngestao.importar(props.legislatura()));
		etapas.add(deputadoIngestao.importar(props.legislatura()));

		return new ResultadoIngestao(inicio, LocalDateTime.now(), etapas);
	}
}
