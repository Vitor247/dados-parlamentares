package com.example.parlamento.ingestao.service;

import com.example.parlamento.domain.entity.Partido;
import com.example.parlamento.domain.repository.PartidoRepository;
import com.example.parlamento.ingestao.client.CamaraClient;
import com.example.parlamento.ingestao.dto.PartidoCamaraDto;
import com.example.parlamento.ingestao.mapper.PartidoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PartidoIngestaoService {

	static final String RECURSO = "partidos";

	private static final Logger log = LoggerFactory.getLogger(PartidoIngestaoService.class);

	private final CamaraClient camaraClient;
	private final PartidoRepository partidoRepository;
	private final IngestaoLogService logService;
	private final TransactionTemplate transactionTemplate;

	public PartidoIngestaoService(CamaraClient camaraClient, PartidoRepository partidoRepository,
			IngestaoLogService logService, TransactionTemplate transactionTemplate) {
		this.camaraClient = camaraClient;
		this.partidoRepository = partidoRepository;
		this.logService = logService;
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Importa os partidos da legislatura. A chamada HTTP fica fora da transação;
	 * só a gravação ocupa conexão com o banco. Falha aqui aborta a carga base,
	 * porque sem partidos não há como vincular deputados.
	 */
	public ResultadoIngestao.Etapa importar(int legislatura) {
		LocalDateTime inicio = LocalDateTime.now();
		String referencia = "legislatura " + legislatura;
		try {
			List<PartidoCamaraDto> dtos = camaraClient.listarPartidos(legislatura);
			int gravados = transactionTemplate.execute(status -> gravar(dtos));
			logService.sucesso(RECURSO, referencia, gravados, inicio);
			log.info("Ingestão de partidos: {} gravados", gravados);
			return new ResultadoIngestao.Etapa(RECURSO, gravados, 0);
		} catch (RuntimeException e) {
			logService.falha(RECURSO, referencia, e, inicio);
			throw e;
		}
	}

	/** Upsert por id oficial: atualiza os existentes, cria os novos. Nunca apaga. */
	private int gravar(List<PartidoCamaraDto> dtos) {
		LocalDateTime agora = LocalDateTime.now();
		Map<Long, Partido> existentes = partidoRepository
				.findAllById(dtos.stream().map(PartidoCamaraDto::id).toList())
				.stream()
				.collect(Collectors.toMap(Partido::getId, Function.identity()));

		List<Partido> partidos = dtos.stream()
				.map(dto -> {
					Partido partido = existentes.computeIfAbsent(dto.id(), Partido::new);
					PartidoMapper.atualizar(partido, dto, agora);
					return partido;
				})
				.toList();
		partidoRepository.saveAll(partidos);
		return partidos.size();
	}
}
