package com.example.parlamento.ingestao.service;

import com.example.parlamento.exception.ConflitoException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executa a carga base em segundo plano. Na instância gratuita ela leva ~15 min, e o proxy
 * da hospedagem corta requisições que ficam esse tempo sem resposta (502). Assim, o pedido
 * só dispara a carga e devolve um id; o andamento é consultado por requisições curtas.
 *
 * <p>O registro é em memória: se a instância reiniciar, a execução em curso se perde (e a
 * consulta responde 404). Como a carga é idempotente, basta disparar de novo.
 */
@Service
public class ExecucaoCargaBaseService {

	public enum Status { EM_ANDAMENTO, CONCLUIDA, FALHOU }

	/**
	 * @param resultado preenchido quando {@code CONCLUIDA}
	 * @param erro      preenchido quando {@code FALHOU}
	 */
	public record Execucao(
			UUID id,
			Status status,
			LocalDateTime iniciadoEm,
			LocalDateTime finalizadoEm,
			ResultadoIngestao resultado,
			String erro) {
	}

	private static final Logger log = LoggerFactory.getLogger(ExecucaoCargaBaseService.class);
	private static final int EXECUCOES_GUARDADAS = 20;

	private final IngestaoService ingestao;
	private final ExecutorService executor =
			Executors.newSingleThreadExecutor(Thread.ofPlatform().name("carga-base").daemon(true).factory());
	private final AtomicReference<UUID> emAndamento = new AtomicReference<>();
	/** Últimas execuções, da mais antiga para a mais nova; as mais antigas são descartadas. */
	private final Map<UUID, Execucao> execucoes = Collections.synchronizedMap(new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<UUID, Execucao> maisAntiga) {
			return size() > EXECUCOES_GUARDADAS;
		}
	});

	public ExecucaoCargaBaseService(IngestaoService ingestao) {
		this.ingestao = ingestao;
	}

	/** Dispara a carga base e retorna na hora. Uma carga por vez: duas concorreriam pelo mesmo banco. */
	public Execucao iniciar() {
		UUID id = UUID.randomUUID();
		if (!emAndamento.compareAndSet(null, id)) {
			throw new ConflitoException("Já existe uma carga base em andamento (execução " + emAndamento.get() + ")");
		}
		Execucao inicial = new Execucao(id, Status.EM_ANDAMENTO, LocalDateTime.now(), null, null, null);
		execucoes.put(id, inicial);
		executor.execute(() -> executar(inicial));
		return inicial;
	}

	public Optional<Execucao> buscar(UUID id) {
		return Optional.ofNullable(execucoes.get(id));
	}

	private void executar(Execucao inicial) {
		try {
			ResultadoIngestao resultado = ingestao.executarCargaBase();
			execucoes.put(inicial.id(), new Execucao(inicial.id(), Status.CONCLUIDA, inicial.iniciadoEm(),
					LocalDateTime.now(), resultado, null));
		} catch (RuntimeException e) {
			log.error("Carga base {} falhou", inicial.id(), e);
			execucoes.put(inicial.id(), new Execucao(inicial.id(), Status.FALHOU, inicial.iniciadoEm(),
					LocalDateTime.now(), null, e.getMessage()));
		} finally {
			emAndamento.set(null);
		}
	}

	@PreDestroy
	void encerrar() {
		executor.shutdownNow();
	}
}
