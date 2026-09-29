package com.example.parlamento.ingestao;

import com.example.parlamento.IntegracaoTest;
import com.example.parlamento.exception.FonteExternaIndisponivelException;
import com.example.parlamento.ingestao.service.IngestaoService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static com.example.parlamento.ingestao.CamaraFake.BASE;
import static com.example.parlamento.ingestao.CamaraFake.autorDeputado;
import static com.example.parlamento.ingestao.CamaraFake.autorExterno;
import static com.example.parlamento.ingestao.CamaraFake.deputado;
import static com.example.parlamento.ingestao.CamaraFake.partido;
import static com.example.parlamento.ingestao.CamaraFake.proposicao;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ingestão ponta a ponta contra uma API da Câmara simulada (WireMock) e um
 * PostgreSQL real. Cobre os casos da seção 12 que quebram ingestão na vida real.
 */
class IngestaoWireMockTest extends IntegracaoTest {

	private static final long PL = 37906;
	private static final long PT = 36844;
	private static final long DEP_A = 1001;
	private static final long DEP_B = 1002;

	/**
	 * Servidor único para a classe inteira (incluindo as @Nested). Com WireMockExtension,
	 * cada classe aninhada reinicia o servidor em outra porta aleatória, e o contexto do
	 * Spring em cache continuaria apontando para a porta antiga.
	 */
	static final WireMockServer wm = new WireMockServer(wireMockConfig().dynamicPort());

	static {
		wm.start();
	}

	@AfterAll
	static void pararWireMock() {
		wm.stop();
	}

	@DynamicPropertySource
	static void apontarParaWireMock(DynamicPropertyRegistry registry) {
		registry.add("camara.api.base-url", () -> wm.baseUrl() + BASE);
		registry.add("camara.api.retry.atraso-inicial", () -> "1ms");
		registry.add("camara.api.retry.atraso-maximo", () -> "5ms");
	}

	@Autowired
	private IngestaoService ingestao;

	private CamaraFake camara;

	@BeforeEach
	void cenarioBase() {
		wm.resetAll();
		camara = new CamaraFake(wm);
		camara.partidos(partido(PL, "PL"), partido(PT, "PT"));
		camara.deputadosPagina(1, false, deputado(DEP_A, "Deputada A", "PL", PL), deputado(DEP_B, "Deputado B", "PT", PT));
		camara.detalheDeputado(DEP_A, "PL");
		camara.detalheDeputado(DEP_B, "PT");
		camara.semProposicoesPorPadrao();
	}

	@Nested
	class CargaBase {

		@Test
		void deputadoComUriPartidoNullNoDetalheVinculaOPartidoPelaLista() {
			ingestao.executarCargaBase();

			assertThat(partidoDe(DEP_A)).isEqualTo(PL);
			assertThat(partidoDe(DEP_B)).isEqualTo(PT);
			// E o detalhe foi de fato consumido (enriquecimento), mesmo com uriPartido null.
			assertThat(jdbc.queryForObject("select nome_civil from deputado where id = ?", String.class, DEP_A))
					.isEqualTo("NOME CIVIL 1001");
			assertThat(contar("select count(*) from ingestao_log where recurso = 'deputados' and status = 'SUCESSO'"))
					.isEqualTo(2);
		}

		@Test
		void respostaPaginadaComMaisDeUmaPaginaImportaTodas() {
			camara.deputadosPagina(1, true, deputado(DEP_A, "Deputada A", "PL", PL), deputado(DEP_B, "Deputado B", "PT", PT));
			camara.deputadosPagina(2, false, deputado(1003, "Deputado C", "PL", PL));
			camara.detalheDeputado(1003, "PL");

			ingestao.executarCargaBase();

			assertThat(contar("select count(*) from deputado")).isEqualTo(3);
			wm.verify(2, getRequestedFor(urlPathEqualTo(BASE + "/deputados")));
		}

		@Test
		void mesmaProposicaoRetornadaParaDoisDeputadosNaoDuplica() {
			camara.proposicoesDoAutor(DEP_A, proposicao(5001, "PL", 1, 2025));
			camara.proposicoesDoAutor(DEP_B, proposicao(5001, "PL", 1, 2025), proposicao(5002, "PEC", 2, 2025));

			ingestao.executarCargaBase();
			ingestao.executarCargaBase(); // reexecução também não pode duplicar

			assertThat(contar("select count(*) from proposicao")).isEqualTo(2);
			assertThat(contar("select count(*) from proposicao_autor where proposicao_id = 5001")).isEqualTo(2);
			assertThat(contar("select count(*) from proposicao_autor")).isEqualTo(3);
			assertThat(contar("select count(*) from proposicao where detalhe_carregado")).isZero();
		}

		@Test
		void erro429DaFonteERepetidoComBackoff() {
			wm.stubFor(get(urlPathEqualTo(BASE + "/partidos")).inScenario("limite de taxa")
					.whenScenarioStateIs(STARTED)
					.willReturn(status(429))
					.willSetStateTo("liberado"));
			wm.stubFor(get(urlPathEqualTo(BASE + "/partidos")).inScenario("limite de taxa")
					.whenScenarioStateIs("liberado")
					.willReturn(okJson("{\"dados\":[" + partido(PL, "PL") + "," + partido(PT, "PT") + "],\"links\":[]}")));

			ingestao.executarCargaBase();

			wm.verify(2, getRequestedFor(urlPathEqualTo(BASE + "/partidos")));
			assertThat(contar("select count(*) from partido")).isEqualTo(2);
		}

		@Test
		void erro429PersistenteEsgotaAsRetentativasEAbortaComFalhaRegistrada() {
			wm.stubFor(get(urlPathEqualTo(BASE + "/partidos")).willReturn(status(429)));

			assertThatThrownBy(() -> ingestao.executarCargaBase())
					.isInstanceOf(FonteExternaIndisponivelException.class);

			// 1 tentativa + 4 retentativas (application.yml)
			wm.verify(5, getRequestedFor(urlPathEqualTo(BASE + "/partidos")));
			assertThat(contar("select count(*) from ingestao_log where recurso = 'partidos' and status = 'FALHA'"))
					.isEqualTo(1);
			assertThat(contar("select count(*) from deputado")).isZero();
		}

		@Test
		void falhaNoDetalheDeUmDeputadoNaoInterrompeOLote() {
			wm.stubFor(get(urlPathEqualTo(BASE + "/deputados/" + DEP_A)).willReturn(status(500)));

			ingestao.executarCargaBase();

			// Gravado com os dados da lista (partido inclusive), marcado como PARCIAL.
			assertThat(partidoDe(DEP_A)).isEqualTo(PL);
			assertThat(partidoDe(DEP_B)).isEqualTo(PT);
			assertThat(jdbc.queryForObject("select status from ingestao_log where recurso = 'deputados' and referencia = ?",
					String.class, String.valueOf(DEP_A))).isEqualTo("PARCIAL");
		}
	}

	@Nested
	class Enriquecimento {

		private static final long PROP = 5001;

		@BeforeEach
		void proposicaoPendente() {
			camara.proposicoesDoAutor(DEP_A, proposicao(PROP, "PL", 1, 2025));
			ingestao.executarCargaBase();
			camara.detalheProposicao(PROP);
		}

		@Test
		void autorSemUriENaoDeputadoEGravadoSemVinculo() {
			camara.autores(PROP,
					autorDeputado(DEP_A, "Deputada A", 1),
					autorExterno("Poder Executivo", "Órgão do Poder Executivo", 2));

			ingestao.executarEnriquecimento(10);

			Map<String, Object> executivo = jdbc.queryForMap(
					"select deputado_id, tipo, ordem_assinatura from proposicao_autor where nome = 'Poder Executivo'");
			assertThat(executivo.get("deputado_id")).isNull();
			assertThat(executivo.get("tipo")).isEqualTo("Órgão do Poder Executivo");
			assertThat(executivo.get("ordem_assinatura")).isEqualTo(2);
			assertThat(contar("select count(*) from proposicao_autor where proposicao_id = ?", PROP)).isEqualTo(2);
			assertThat(jdbc.queryForObject("select situacao_descricao from proposicao where id = ?", String.class, PROP))
					.isEqualTo("Aguardando Parecer");
			assertThat(contar("select count(*) from proposicao where detalhe_carregado")).isEqualTo(1);
		}

		@Test
		void autorApontandoParaDeputadoInexistenteNaBaseFicaSemVinculoENaoCriaDeputado() {
			camara.autores(PROP,
					autorDeputado(DEP_A, "Deputada A", 1),
					autorDeputado(9999, "Ex-Deputado", 2));

			ingestao.executarEnriquecimento(10);

			assertThat(jdbc.queryForMap("select deputado_id, nome from proposicao_autor where ordem_assinatura = 2"))
					.containsEntry("deputado_id", null)
					.containsEntry("nome", "Ex-Deputado");
			assertThat(contar("select count(*) from deputado where id = 9999")).isZero();
			assertThat(jdbc.queryForObject(
					"select mensagem_erro from ingestao_log where recurso = 'enriquecimento' and status = 'PARCIAL'",
					String.class)).contains("9999");
		}

		@Test
		void reenriquecerNaoDuplicaAutoresNaoDeputados() {
			// NULL não conflita com NULL na unique: só apagar-e-regravar evita duplicata.
			camara.autores(PROP,
					autorDeputado(DEP_A, "Deputada A", 1),
					autorExterno("Senado Federal", "Senado Federal", 2));

			ingestao.executarEnriquecimento(10);
			jdbc.update("update proposicao set detalhe_carregado = false where id = ?", PROP);
			ingestao.executarEnriquecimento(10);

			assertThat(contar("select count(*) from proposicao_autor where proposicao_id = ?", PROP)).isEqualTo(2);
		}

		@Test
		void fonteSemAutoresMantemAAutoriaDaFaseA() {
			camara.autores(PROP);

			ingestao.executarEnriquecimento(10);

			assertThat(contar("select count(*) from proposicao_autor where proposicao_id = ? and deputado_id = ?",
					PROP, DEP_A)).isEqualTo(1);
			assertThat(contar("select count(*) from proposicao where detalhe_carregado")).isEqualTo(1);
		}

		@Test
		void falhaNaFonteDeixaAProposicaoPendenteParaOProximoLote() {
			wm.stubFor(get(urlPathEqualTo(BASE + "/proposicoes/" + PROP + "/autores")).willReturn(status(503)));

			var resultado = ingestao.executarEnriquecimento(10);

			assertThat(resultado.etapas().getFirst().falhas()).isEqualTo(1);
			assertThat(resultado.proposicoesPendentes()).isEqualTo(1);
			assertThat(contar("select count(*) from proposicao where detalhe_carregado")).isZero();
		}

		@Test
		void situacaoDesatualizadaEBuscadaDeNovo() {
			camara.autores(PROP, autorDeputado(DEP_A, "Deputada A", 1));
			ingestao.executarEnriquecimento(10);
			envelhecerSituacao(PROP, 10);

			// Na Câmara, a proposição andou.
			camara.detalheProposicao(PROP, "Aprovada");
			var resultado = ingestao.executarEnriquecimento(10);

			assertThat(resultado.etapas().getFirst().processados()).isEqualTo(1);
			assertThat(jdbc.queryForObject("select situacao_descricao from proposicao where id = ?", String.class, PROP))
					.isEqualTo("Aprovada");
			assertThat(resultado.proposicoesDesatualizadas()).isZero();
		}

		@Test
		void situacaoRecenteNaoEBuscadaDeNovo() {
			camara.autores(PROP, autorDeputado(DEP_A, "Deputada A", 1));
			ingestao.executarEnriquecimento(10);

			var resultado = ingestao.executarEnriquecimento(10);

			assertThat(resultado.etapas().getFirst().processados()).isZero();
			wm.verify(1, getRequestedFor(urlPathEqualTo(BASE + "/proposicoes/" + PROP)));
		}

		@Test
		void nuncaEnriquecidasTemPrioridadeSobreDesatualizadas() {
			camara.autores(PROP, autorDeputado(DEP_A, "Deputada A", 1));
			ingestao.executarEnriquecimento(10);
			envelhecerSituacao(PROP, 10);
			// Uma proposição nova, ainda sem detalhe.
			jdbc.update("""
					insert into proposicao (id, sigla_tipo, numero, ano, detalhe_carregado, atualizado_em)
					values (5002, 'PL', 2, 2025, false, now())""");
			camara.detalheProposicao(5002);
			camara.autores(5002, autorDeputado(DEP_A, "Deputada A", 1));

			var resultado = ingestao.executarEnriquecimento(1);

			assertThat(contar("select count(*) from proposicao where id = 5002 and detalhe_carregado")).isEqualTo(1);
			assertThat(resultado.proposicoesPendentes()).isZero();
			assertThat(resultado.proposicoesDesatualizadas()).isEqualTo(1);
		}

		private void envelhecerSituacao(long proposicaoId, int dias) {
			jdbc.update("update proposicao set detalhe_atualizado_em = detalhe_atualizado_em - make_interval(days => ?) where id = ?",
					dias, proposicaoId);
		}
	}

	private Long partidoDe(long deputadoId) {
		return jdbc.queryForObject("select partido_id from deputado where id = ?", Long.class, deputadoId);
	}
}
