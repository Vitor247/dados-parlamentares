package com.example.parlamento.ingestao.service;

import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.entity.Partido;
import com.example.parlamento.domain.repository.DeputadoRepository;
import com.example.parlamento.domain.repository.PartidoRepository;
import com.example.parlamento.exception.FonteExternaIndisponivelException;
import com.example.parlamento.ingestao.client.CamaraClient;
import com.example.parlamento.ingestao.dto.DeputadoDetalheCamaraDto;
import com.example.parlamento.ingestao.dto.DeputadoResumoCamaraDto;
import com.example.parlamento.ingestao.mapper.DeputadoMapper;
import com.example.parlamento.ingestao.mapper.UriCamara;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeputadoIngestaoService {

	static final String RECURSO = "deputados";

	private static final Logger log = LoggerFactory.getLogger(DeputadoIngestaoService.class);

	private final CamaraClient camaraClient;
	private final DeputadoRepository deputadoRepository;
	private final PartidoRepository partidoRepository;
	private final IngestaoLogService logService;
	private final TransactionTemplate transactionTemplate;

	public DeputadoIngestaoService(CamaraClient camaraClient, DeputadoRepository deputadoRepository,
			PartidoRepository partidoRepository, IngestaoLogService logService,
			TransactionTemplate transactionTemplate) {
		this.camaraClient = camaraClient;
		this.deputadoRepository = deputadoRepository;
		this.partidoRepository = partidoRepository;
		this.logService = logService;
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Importa os deputados da legislatura: lista (com o vínculo de partido) + detalhe.
	 * Cada deputado é uma unidade independente: transação e linha de log próprias,
	 * e a falha de um não interrompe os demais.
	 */
	public ResultadoIngestao.Etapa importar(int legislatura) {
		List<DeputadoResumoCamaraDto> lista;
		try {
			lista = camaraClient.listarDeputados(legislatura);
		} catch (RuntimeException e) {
			logService.falha(RECURSO, "legislatura " + legislatura, e, LocalDateTime.now());
			throw e;
		}

		// A lista repete o deputado uma vez por partido na legislatura; agrupa mantendo a ordem da fonte.
		Map<Long, List<DeputadoResumoCamaraDto>> registrosPorDeputado = lista.stream()
				.collect(Collectors.groupingBy(DeputadoResumoCamaraDto::id, LinkedHashMap::new, Collectors.toList()));
		Set<Long> partidosConhecidos = partidoRepository.findAllIds();
		log.info("Ingestão de deputados: {} registros na lista, {} deputados distintos",
				lista.size(), registrosPorDeputado.size());

		int gravados = 0;
		int falhas = 0;
		for (Map.Entry<Long, List<DeputadoResumoCamaraDto>> entrada : registrosPorDeputado.entrySet()) {
			if (importarDeputado(entrada.getKey(), entrada.getValue(), partidosConhecidos)) {
				gravados++;
			} else {
				falhas++;
			}
		}
		log.info("Ingestão de deputados: {} gravados, {} falhas", gravados, falhas);
		return new ResultadoIngestao.Etapa(RECURSO, gravados, falhas);
	}

	private boolean importarDeputado(Long id, List<DeputadoResumoCamaraDto> registros, Set<Long> partidosConhecidos) {
		LocalDateTime inicio = LocalDateTime.now();
		String referencia = String.valueOf(id);
		List<String> pendencias = new ArrayList<>();
		try {
			DeputadoDetalheCamaraDto detalhe = buscarDetalhe(id, pendencias);
			DeputadoResumoCamaraDto atual = DeputadoMapper.registroAtual(registros, detalhe);

			Long partidoId = UriCamara.extrairId(atual.uriPartido())
					.filter(partidosConhecidos::contains)
					.orElse(null);
			if (partidoId == null) {
				pendencias.add("partido não resolvido (sigla=" + atual.siglaPartido() + ", uri=" + atual.uriPartido() + ")");
			}

			transactionTemplate.executeWithoutResult(status -> gravar(id, atual, detalhe, partidoId));

			if (pendencias.isEmpty()) {
				logService.sucesso(RECURSO, referencia, 1, inicio);
			} else {
				logService.parcial(RECURSO, referencia, 1, String.join("; ", pendencias), inicio);
			}
			return true;
		} catch (RuntimeException e) {
			log.warn("Falha ao importar deputado {}: {}", id, e.getMessage());
			logService.falha(RECURSO, referencia, e, inicio);
			return false;
		}
	}

	/** O detalhe só enriquece: se não vier, o deputado é gravado com os dados da lista. */
	private DeputadoDetalheCamaraDto buscarDetalhe(Long id, List<String> pendencias) {
		try {
			DeputadoDetalheCamaraDto detalhe = camaraClient.buscarDeputado(id).orElse(null);
			if (detalhe == null) {
				pendencias.add("detalhe não encontrado na fonte");
			}
			return detalhe;
		} catch (FonteExternaIndisponivelException e) {
			pendencias.add("detalhe indisponível: " + e.getMessage());
			return null;
		}
	}

	/** Upsert por id oficial. Sem detalhe, preserva o enriquecimento de cargas anteriores. */
	private void gravar(Long id, DeputadoResumoCamaraDto resumo, DeputadoDetalheCamaraDto detalhe, Long partidoId) {
		Deputado deputado = deputadoRepository.findById(id).orElseGet(() -> new Deputado(id));
		Partido partido = partidoId == null ? null : partidoRepository.getReferenceById(partidoId);
		DeputadoMapper.atualizarDoResumo(deputado, resumo, partido, LocalDateTime.now());
		if (detalhe != null) {
			DeputadoMapper.atualizarDoDetalhe(deputado, detalhe);
		}
		deputadoRepository.save(deputado);
	}
}
