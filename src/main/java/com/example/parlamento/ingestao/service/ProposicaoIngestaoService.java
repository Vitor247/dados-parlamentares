package com.example.parlamento.ingestao.service;

import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.entity.Proposicao;
import com.example.parlamento.domain.entity.ProposicaoAutor;
import com.example.parlamento.domain.repository.DeputadoRepository;
import com.example.parlamento.domain.repository.ProposicaoAutorRepository;
import com.example.parlamento.domain.repository.ProposicaoRepository;
import com.example.parlamento.ingestao.client.CamaraClient;
import com.example.parlamento.ingestao.dto.ProposicaoResumoCamaraDto;
import com.example.parlamento.ingestao.mapper.ProposicaoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fase A de proposições: para cada deputado da base, busca as proposições de sua
 * autoria ({@code idDeputadoAutor}) e grava proposição + vínculo de autoria.
 * Situação, data e coautores ficam para o enriquecimento (Fase B).
 */
@Service
public class ProposicaoIngestaoService {

	static final String RECURSO = "proposicoes";

	private static final Logger log = LoggerFactory.getLogger(ProposicaoIngestaoService.class);

	private final CamaraClient camaraClient;
	private final DeputadoRepository deputadoRepository;
	private final ProposicaoRepository proposicaoRepository;
	private final ProposicaoAutorRepository autorRepository;
	private final IngestaoLogService logService;
	private final TransactionTemplate transactionTemplate;

	public ProposicaoIngestaoService(CamaraClient camaraClient, DeputadoRepository deputadoRepository,
			ProposicaoRepository proposicaoRepository, ProposicaoAutorRepository autorRepository,
			IngestaoLogService logService, TransactionTemplate transactionTemplate) {
		this.camaraClient = camaraClient;
		this.deputadoRepository = deputadoRepository;
		this.proposicaoRepository = proposicaoRepository;
		this.autorRepository = autorRepository;
		this.logService = logService;
		this.transactionTemplate = transactionTemplate;
	}

	/** Cada deputado é uma unidade: transação e linha de log próprias; falha em um não aborta o lote. */
	public ResultadoIngestao.Etapa importar(LocalDate dataApresentacaoInicio, List<String> tipos) {
		List<Long> deputadoIds = deputadoRepository.findAllIds();
		log.info("Ingestão de proposições: {} deputados, apresentadas desde {}, tipos {}",
				deputadoIds.size(), dataApresentacaoInicio, tipos);

		int deputadosOk = 0;
		int falhas = 0;
		int proposicoes = 0;
		for (Long deputadoId : deputadoIds) {
			LocalDateTime inicio = LocalDateTime.now();
			String referencia = "deputado " + deputadoId;
			try {
				List<ProposicaoResumoCamaraDto> dtos =
						camaraClient.listarProposicoesPorAutor(deputadoId, dataApresentacaoInicio, tipos);
				transactionTemplate.executeWithoutResult(status -> gravar(deputadoId, dtos));
				logService.sucesso(RECURSO, referencia, dtos.size(), inicio);
				deputadosOk++;
				proposicoes += dtos.size();
			} catch (RuntimeException e) {
				log.warn("Falha ao importar proposições do deputado {}: {}", deputadoId, e.getMessage());
				logService.falha(RECURSO, referencia, e, inicio);
				falhas++;
			}
		}
		log.info("Ingestão de proposições: {} deputados processados, {} falhas, {} vínculos de autoria lidos",
				deputadosOk, falhas, proposicoes);
		return new ResultadoIngestao.Etapa(RECURSO, deputadosOk, falhas);
	}

	private void gravar(Long deputadoId, List<ProposicaoResumoCamaraDto> dtos) {
		if (dtos.isEmpty()) {
			return;
		}
		LocalDateTime agora = LocalDateTime.now();
		Deputado deputado = deputadoRepository.getReferenceById(deputadoId);
		List<Long> ids = dtos.stream().map(ProposicaoResumoCamaraDto::id).distinct().toList();

		// Upsert por id: a mesma proposição pode ter vindo antes, pela autoria de outro deputado.
		Map<Long, Proposicao> existentes = proposicaoRepository.findAllById(ids).stream()
				.collect(Collectors.toMap(Proposicao::getId, Function.identity()));
		for (ProposicaoResumoCamaraDto dto : dtos) {
			Proposicao proposicao = existentes.computeIfAbsent(dto.id(), Proposicao::new);
			ProposicaoMapper.atualizarDoResumo(proposicao, dto, agora);
		}
		// Com id atribuído, save() faz merge e devolve outra instância: é ela que fica gerenciada.
		Map<Long, Proposicao> proposicoes = proposicaoRepository.saveAll(existentes.values()).stream()
				.collect(Collectors.toMap(Proposicao::getId, Function.identity()));

		// Vínculo de autoria só se ainda não existir; ordem/proponente chegam na Fase B.
		Set<Long> jaVinculadas = autorRepository.findProposicaoIdsComAutor(deputadoId, ids);
		List<ProposicaoAutor> novos = ids.stream()
				.filter(id -> !jaVinculadas.contains(id))
				.map(id -> {
					ProposicaoAutor autor = new ProposicaoAutor(proposicoes.get(id), deputado.getNome());
					autor.setDeputado(deputado);
					return autor;
				})
				.toList();
		autorRepository.saveAll(novos);
	}
}
