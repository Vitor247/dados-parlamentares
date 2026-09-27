package com.example.parlamento.ingestao.service;

import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.entity.Proposicao;
import com.example.parlamento.domain.entity.ProposicaoAutor;
import com.example.parlamento.domain.repository.DeputadoRepository;
import com.example.parlamento.domain.repository.ProposicaoAutorRepository;
import com.example.parlamento.domain.repository.ProposicaoRepository;
import com.example.parlamento.ingestao.client.CamaraClient;
import com.example.parlamento.ingestao.dto.AutorCamaraDto;
import com.example.parlamento.ingestao.dto.ProposicaoDetalheCamaraDto;
import com.example.parlamento.ingestao.mapper.AutorMapper;
import com.example.parlamento.ingestao.mapper.ProposicaoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fase B: completa proposições com {@code detalhe_carregado = false} — situação,
 * data de apresentação, inteiro teor e autoria completa (coautores e não-deputados).
 * Roda em lotes; é retomável e interrompível: o que falha continua pendente.
 */
@Service
public class EnriquecimentoService {

	static final String RECURSO = "enriquecimento";

	private static final Logger log = LoggerFactory.getLogger(EnriquecimentoService.class);

	private final CamaraClient camaraClient;
	private final ProposicaoRepository proposicaoRepository;
	private final ProposicaoAutorRepository autorRepository;
	private final DeputadoRepository deputadoRepository;
	private final IngestaoLogService logService;
	private final TransactionTemplate transactionTemplate;

	public EnriquecimentoService(CamaraClient camaraClient, ProposicaoRepository proposicaoRepository,
			ProposicaoAutorRepository autorRepository, DeputadoRepository deputadoRepository,
			IngestaoLogService logService, TransactionTemplate transactionTemplate) {
		this.camaraClient = camaraClient;
		this.proposicaoRepository = proposicaoRepository;
		this.autorRepository = autorRepository;
		this.deputadoRepository = deputadoRepository;
		this.logService = logService;
		this.transactionTemplate = transactionTemplate;
	}

	public ResultadoIngestao.Etapa enriquecer(int limite) {
		List<Long> pendentes = proposicaoRepository.findIdsPendentes(PageRequest.of(0, limite));
		Set<Long> deputadosNaBase = new HashSet<>(deputadoRepository.findAllIds());
		log.info("Enriquecimento: {} proposições neste lote (limite {})", pendentes.size(), limite);

		int enriquecidas = 0;
		int falhas = 0;
		for (Long id : pendentes) {
			if (enriquecer(id, deputadosNaBase)) {
				enriquecidas++;
			} else {
				falhas++;
			}
		}
		log.info("Enriquecimento: {} enriquecidas, {} falhas", enriquecidas, falhas);
		return new ResultadoIngestao.Etapa(RECURSO, enriquecidas, falhas);
	}

	/** Uma proposição = uma unidade: HTTP fora da transação, gravação em transação própria. */
	private boolean enriquecer(Long id, Set<Long> deputadosNaBase) {
		LocalDateTime inicio = LocalDateTime.now();
		String referencia = String.valueOf(id);
		List<String> pendencias = new ArrayList<>();
		try {
			ProposicaoDetalheCamaraDto detalhe = camaraClient.buscarProposicao(id)
					.orElseThrow(() -> new IllegalStateException("proposição não encontrada na fonte"));
			List<AutorCamaraDto> autores = camaraClient.listarAutores(id);

			int gravados = transactionTemplate.execute(status -> gravar(id, detalhe, autores, deputadosNaBase, pendencias));

			if (pendencias.isEmpty()) {
				logService.sucesso(RECURSO, referencia, gravados, inicio);
			} else {
				logService.parcial(RECURSO, referencia, gravados, String.join("; ", pendencias), inicio);
			}
			return true;
		} catch (RuntimeException e) {
			log.warn("Falha ao enriquecer proposição {}: {}", id, e.getMessage());
			logService.falha(RECURSO, referencia, e, inicio);
			return false;
		}
	}

	/** @return quantidade de autores gravados */
	private int gravar(Long id, ProposicaoDetalheCamaraDto detalhe, List<AutorCamaraDto> autores,
			Set<Long> deputadosNaBase, List<String> pendencias) {
		Proposicao proposicao = proposicaoRepository.findById(id)
				.orElseThrow(() -> new IllegalStateException("proposição " + id + " não está mais na base"));
		ProposicaoMapper.atualizarDoDetalhe(proposicao, detalhe, LocalDateTime.now());

		if (autores.isEmpty()) {
			// Apagar e não regravar nada destruiria o vínculo da Fase A.
			pendencias.add("fonte não retornou autores; autoria existente mantida");
			return 0;
		}

		// Apaga e regrava (não upsert): com deputado_id null, a unique não impede duplicatas.
		autorRepository.deleteByProposicaoId(id);
		Set<Long> deputadosVinculados = new HashSet<>();
		List<ProposicaoAutor> novos = new ArrayList<>();
		for (AutorCamaraDto dto : autores) {
			if (dto.nome() == null || dto.nome().isBlank()) {
				pendencias.add("autor sem nome ignorado (uri=" + dto.uri() + ")");
				continue;
			}
			Long deputadoId = AutorMapper.idDeputado(dto).orElse(null);
			if (deputadoId != null && !deputadosNaBase.contains(deputadoId)) {
				// Ex-deputado de outra legislatura, por exemplo: grava sem vínculo, nunca cria deputado.
				pendencias.add("deputado " + deputadoId + " (" + dto.nome() + ") fora da base; gravado sem vínculo");
				deputadoId = null;
			}
			if (deputadoId != null && !deputadosVinculados.add(deputadoId)) {
				continue; // mesmo deputado repetido na fonte
			}
			Deputado deputado = deputadoId == null ? null : deputadoRepository.getReferenceById(deputadoId);
			novos.add(AutorMapper.paraEntidade(proposicao, dto, deputado));
		}
		autorRepository.saveAll(novos);
		return novos.size();
	}
}
