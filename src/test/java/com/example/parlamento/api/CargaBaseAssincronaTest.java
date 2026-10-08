package com.example.parlamento.api;

import com.example.parlamento.IntegracaoTest;
import com.example.parlamento.exception.FonteExternaIndisponivelException;
import com.example.parlamento.ingestao.service.IngestaoService;
import com.example.parlamento.ingestao.service.ResultadoIngestao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Carga base assíncrona: o POST só dispara e devolve um id; o andamento é consultado à parte.
 * A ingestão real é trocada por um mock controlado por latch, para os estados serem determinísticos.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "parlamento.seguranca.admin-api-key=" + CargaBaseAssincronaTest.CHAVE)
class CargaBaseAssincronaTest extends IntegracaoTest {

	static final String CHAVE = "chave-teste-assincrona";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private IngestaoService ingestao;

	/** A carga simulada só termina quando o teste libera. */
	private final CountDownLatch liberar = new CountDownLatch(1);

	@AfterEach
	void liberarCargaPendente() {
		liberar.countDown();
	}

	@Test
	void disparaRespondeNaHoraEDepoisInformaOResultado() throws Exception {
		ResultadoIngestao resultado = new ResultadoIngestao(LocalDateTime.now(), LocalDateTime.now(),
				List.of(new ResultadoIngestao.Etapa("partidos", 27, 0)), 0, 0);
		when(ingestao.executarCargaBase()).thenAnswer(inv -> {
			liberar.await(10, TimeUnit.SECONDS);
			return resultado;
		});

		MvcTestResult disparo = post("/api/v1/admin/ingestao/base");
		assertThat(disparo).hasStatus(HttpStatus.ACCEPTED)
				.bodyJson().extractingPath("$.status").isEqualTo("EM_ANDAMENTO");
		String id = id(disparo);
		assertThat(disparo.getResponse().getHeader("Location")).isEqualTo("/api/v1/admin/ingestao/execucoes/" + id);

		assertThat(get("/api/v1/admin/ingestao/execucoes/" + id)).bodyJson()
				.extractingPath("$.status").isEqualTo("EM_ANDAMENTO");

		liberar.countDown();
		Map<String, Object> final_ = aguardarFim(id);
		assertThat(final_).containsEntry("status", "CONCLUIDA").containsEntry("erro", null);
		assertThat(get("/api/v1/admin/ingestao/execucoes/" + id)).bodyJson()
				.extractingPath("$.resultado.etapas[0].processados").isEqualTo(27);
	}

	@Test
	void segundaCargaEnquantoAPrimeiraRodaE409() {
		when(ingestao.executarCargaBase()).thenAnswer(inv -> {
			liberar.await(10, TimeUnit.SECONDS);
			return null;
		});

		assertThat(post("/api/v1/admin/ingestao/base")).hasStatus(HttpStatus.ACCEPTED);
		assertThat(post("/api/v1/admin/ingestao/base")).hasStatus(HttpStatus.CONFLICT)
				.bodyJson().extractingPath("$.erro").isEqualTo("Conflito");
	}

	@Test
	void falhaDaCargaFicaRegistradaNaExecucao() throws Exception {
		when(ingestao.executarCargaBase()).thenThrow(
				new FonteExternaIndisponivelException("Falha ao consultar /partidos página 1 na API da Câmara", null));

		String id = id(post("/api/v1/admin/ingestao/base"));

		assertThat(aguardarFim(id))
				.containsEntry("status", "FALHOU")
				.containsEntry("erro", "Falha ao consultar /partidos página 1 na API da Câmara");
	}

	@Test
	void execucaoDesconhecidaE404() {
		assertThat(get("/api/v1/admin/ingestao/execucoes/00000000-0000-0000-0000-000000000000"))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void consultaDeExecucaoTambemExigeAChave() {
		assertThat(mvc.get().uri("/api/v1/admin/ingestao/execucoes/00000000-0000-0000-0000-000000000000").exchange())
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	/** Consulta até sair de EM_ANDAMENTO (a carga roda em outra thread). */
	@SuppressWarnings("unchecked")
	private Map<String, Object> aguardarFim(String id) throws InterruptedException {
		for (int i = 0; i < 100; i++) {
			MvcTestResult r = get("/api/v1/admin/ingestao/execucoes/" + id);
			Map<String, Object> corpo = (Map<String, Object>) assertThat(r).bodyJson().convertTo(Map.class).actual();
			if (!"EM_ANDAMENTO".equals(corpo.get("status"))) {
				return corpo;
			}
			Thread.sleep(50);
		}
		throw new AssertionError("A execução " + id + " não terminou a tempo");
	}

	@SuppressWarnings("unchecked")
	private String id(MvcTestResult disparo) {
		return (String) ((Map<String, Object>) assertThat(disparo).bodyJson().convertTo(Map.class).actual()).get("id");
	}

	private MvcTestResult post(String uri) {
		return mvc.post().uri(uri).header("X-Admin-Key", CHAVE).exchange();
	}

	private MvcTestResult get(String uri) {
		return mvc.get().uri(uri).header("X-Admin-Key", CHAVE).exchange();
	}
}
