package com.example.parlamento.ingestao.service;

import com.example.parlamento.config.IngestaoProperties;
import com.example.parlamento.domain.repository.ProposicaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** Orquestra as fases de ingestão na ordem exigida pelas dependências entre recursos. */
@Service
public class IngestaoService {

	private final IngestaoProperties props;
	private final PartidoIngestaoService partidoIngestao;
	private final DeputadoIngestaoService deputadoIngestao;
	private final ProposicaoIngestaoService proposicaoIngestao;
	private final EnriquecimentoService enriquecimento;
	private final ProposicaoRepository proposicaoRepository;

	public IngestaoService(IngestaoProperties props, PartidoIngestaoService partidoIngestao,
			DeputadoIngestaoService deputadoIngestao, ProposicaoIngestaoService proposicaoIngestao,
			EnriquecimentoService enriquecimento, ProposicaoRepository proposicaoRepository) {
		this.props = props;
		this.partidoIngestao = partidoIngestao;
		this.deputadoIngestao = deputadoIngestao;
		this.proposicaoIngestao = proposicaoIngestao;
		this.enriquecimento = enriquecimento;
		this.proposicaoRepository = proposicaoRepository;
	}

	/** Fase A — carga base: partidos → deputados → proposições (com vínculo de autoria). */
	public ResultadoIngestao executarCargaBase() {
		LocalDateTime inicio = LocalDateTime.now();
		List<ResultadoIngestao.Etapa> etapas = List.of(
				partidoIngestao.importar(props.legislatura()),
				deputadoIngestao.importar(props.legislatura()),
				proposicaoIngestao.importar(props.dataApresentacaoInicio(), props.tiposProposicao()));
		return resultado(inicio, etapas);
	}

	/** Fase B — enriquecimento de até {@code limite} proposições pendentes. */
	public ResultadoIngestao executarEnriquecimento(int limite) {
		if (limite < 1) {
			throw new IllegalArgumentException("limite deve ser maior que zero");
		}
		LocalDateTime inicio = LocalDateTime.now();
		return resultado(inicio, List.of(enriquecimento.enriquecer(limite)));
	}

	private ResultadoIngestao resultado(LocalDateTime inicio, List<ResultadoIngestao.Etapa> etapas) {
		return new ResultadoIngestao(inicio, LocalDateTime.now(), etapas,
				proposicaoRepository.countByDetalheCarregadoFalse());
	}
}
