package com.example.parlamento.api;

import com.example.parlamento.IntegracaoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API pública ponta a ponta: MockMvc sobre PostgreSQL real (Testcontainers).
 *
 * <pre>
 * Massa de dados:
 *   Partidos: PL (37906), PT (36844)
 *   Deputados: 1001 Ana Silva (PL-MG), 1002 Bruno Souza (PT-SP),
 *              1003 Carlos Silveira (MG, sigla "XYZ" sem partido resolvido)
 *   Proposições: 5001 PL 100/2025 (enriquecida; autores: Ana, Poder Executivo, Bruno)
 *                5002 PEC 7/2026  (pendente;  autora: Ana; ementa com "50%")
 *                5003 PL 200/2026 (pendente;  autor: Bruno)
 * </pre>
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"parlamento.seguranca.admin-api-key=" + ApiIntegracaoTest.CHAVE_ADMIN,
		"parlamento.seguranca.cors-origens=https://front.exemplo.org"})
class ApiIntegracaoTest extends IntegracaoTest {

	static final String CHAVE_ADMIN = "chave-de-teste";

	@Autowired
	private MockMvcTester mvc;

	@BeforeEach
	void massaDeDados() {
		jdbc.update("""
				insert into partido (id, sigla, nome, uri, atualizado_em) values
				  (37906, 'PL', 'Partido Liberal', 'https://dadosabertos.camara.leg.br/api/v2/partidos/37906', now()),
				  (36844, 'PT', 'Partido dos Trabalhadores', 'https://dadosabertos.camara.leg.br/api/v2/partidos/36844', now())""");
		jdbc.update("""
				insert into deputado (id, nome, nome_civil, sigla_uf, id_legislatura, email, data_nascimento,
				                      situacao, partido_id, sigla_partido, atualizado_em) values
				  (1001, 'Ana Silva', 'ANA SILVA', 'MG', 57, null, '1980-01-01', 'Exercício', 37906, 'PL', now()),
				  (1002, 'Bruno Souza', 'BRUNO SOUZA', 'SP', 57, 'dep.bruno@camara.leg.br', null, 'Exercício', 36844, 'PT', now()),
				  (1003, 'Carlos Silveira', null, 'MG', 57, null, null, 'Suplência', null, 'XYZ', now())""");
		jdbc.update("""
				insert into proposicao (id, sigla_tipo, cod_tipo, descricao_tipo, numero, ano, ementa, data_apresentacao,
				                        url_inteiro_teor, situacao_descricao, situacao_cod, situacao_data,
				                        situacao_orgao_sigla, tramitacao_descricao, detalhe_carregado, detalhe_atualizado_em,
				                        atualizado_em) values
				  (5001, 'PL', 139, 'Projeto de Lei', 100, 2025, 'Dispõe sobre a Saúde pública', '2025-04-09 18:16',
				   'https://www.camara.leg.br/teor/5001', 'Aguardando Parecer', 1100, '2025-04-27 00:00',
				   'CCJC', 'Recebimento', true, '2026-09-20 03:00', now()),
				  (5002, 'PEC', 136, null, 7, 2026, 'Reduz em 50% a alíquota', null, null, null, null, null, null, null, false, null, now()),
				  (5003, 'PL', 139, null, 200, 2026, 'Institui o dia nacional', null, null, null, null, null, null, null, false, null, now())""");
		jdbc.update("""
				insert into proposicao_autor (proposicao_id, deputado_id, nome, tipo, cod_tipo, ordem_assinatura, proponente) values
				  (5001, 1001, 'Ana Silva', 'Deputado(a)', 10000, 1, 1),
				  (5001, null, 'Poder Executivo', 'Órgão do Poder Executivo', 40000, 2, 1),
				  (5001, 1002, 'Bruno Souza', 'Deputado(a)', 10000, 3, 0),
				  (5002, 1001, 'Ana Silva', null, null, null, null),
				  (5003, 1002, 'Bruno Souza', null, null, null, null)""");
	}

	@Nested
	class Deputados {

		@Test
		void listagemVemNoEnvelopePaginadoComFonte() {
			var resposta = assertThat(get("/api/v1/deputados")).hasStatusOk().bodyJson();

			resposta.extractingPath("$.conteudo[*].nome").asArray()
					.containsExactly("Ana Silva", "Bruno Souza", "Carlos Silveira");
			resposta.extractingPath("$.pagina").isEqualTo(0);
			resposta.extractingPath("$.tamanho").isEqualTo(20);
			resposta.extractingPath("$.totalElementos").isEqualTo(3);
			resposta.extractingPath("$.totalPaginas").isEqualTo(1);
			resposta.extractingPath("$._fonte.origem").isEqualTo("Câmara dos Deputados - Dados Abertos");
			resposta.extractingPath("$.conteudo[0]._fonte.uri")
					.isEqualTo("https://dadosabertos.camara.leg.br/api/v2/deputados/1001");
		}

		@Test
		void tamanhoDePaginaTemTetoDe100() {
			assertThat(get("/api/v1/deputados?size=500")).bodyJson().extractingPath("$.tamanho").isEqualTo(100);
		}

		@Test
		void paginacaoComecaEmZero() {
			var resposta = assertThat(get("/api/v1/deputados?size=2&page=1")).hasStatusOk().bodyJson();
			resposta.extractingPath("$.conteudo[*].nome").asArray().containsExactly("Carlos Silveira");
			resposta.extractingPath("$.totalPaginas").isEqualTo(2);
		}

		@Test
		void filtraPorUfPartidoENomeSemDiferenciarMaiusculas() {
			assertThat(get("/api/v1/deputados?uf=mg")).bodyJson()
					.extractingPath("$.conteudo[*].id").asArray().containsExactly(1001, 1003);
			assertThat(get("/api/v1/deputados?partido=pt")).bodyJson()
					.extractingPath("$.conteudo[*].id").asArray().containsExactly(1002);
			assertThat(get("/api/v1/deputados?nome=SILV")).bodyJson()
					.extractingPath("$.conteudo[*].id").asArray().containsExactly(1001, 1003);
			assertThat(get("/api/v1/deputados?uf=MG&partido=PL&nome=ana")).bodyJson()
					.extractingPath("$.conteudo[*].id").asArray().containsExactly(1001);
		}

		@Test
		void siglaDePartidoInexistenteNaoRetornaNinguem() {
			// "XYZ" só existe como retrato em sigla_partido, sem partido resolvido na base.
			assertThat(get("/api/v1/deputados?partido=XYZ")).bodyJson().extractingPath("$.totalElementos").isEqualTo(0);
		}

		@Test
		void detalheTrazTotalProposicoesECamposNulosPresentes() {
			var resposta = assertThat(get("/api/v1/deputados/1001")).hasStatusOk().bodyJson();

			resposta.extractingPath("$.nomeCivil").isEqualTo("ANA SILVA");
			resposta.extractingPath("$.partido.sigla").isEqualTo("PL");
			resposta.extractingPath("$.totalProposicoes").isEqualTo(2);
			// Ausência de dado é informação: o campo aparece com null, não some.
			resposta.extractingPath("$").asMap().containsEntry("email", null).containsKey("escolaridade");
		}

		@Test
		void deputadoSemPartidoResolvidoMantemASiglaDaFonte() {
			var resposta = assertThat(get("/api/v1/deputados/1003")).hasStatusOk().bodyJson();

			resposta.extractingPath("$.siglaPartido").isEqualTo("XYZ");
			resposta.extractingPath("$").asMap().containsEntry("partido", null);
			resposta.extractingPath("$.totalProposicoes").isEqualTo(0);
		}

		@Test
		void proposicoesDoDeputadoComFiltros() {
			assertThat(get("/api/v1/deputados/1001/proposicoes")).bodyJson()
					.extractingPath("$.conteudo[*].identificacao").asArray().containsExactly("PEC 7/2026", "PL 100/2025");
			assertThat(get("/api/v1/deputados/1001/proposicoes?tipo=pec")).bodyJson()
					.extractingPath("$.conteudo[*].id").asArray().containsExactly(5002);
			assertThat(get("/api/v1/deputados/1001/proposicoes?ano=2025")).bodyJson()
					.extractingPath("$.conteudo[*].id").asArray().containsExactly(5001);
		}

		@Test
		void proposicoesDeDeputadoInexistenteE404() {
			assertThat(get("/api/v1/deputados/999999/proposicoes")).hasStatus(HttpStatus.NOT_FOUND);
		}
	}

	@Nested
	class Partidos {

		@Test
		void listaOrdenadaPorSigla() {
			assertThat(get("/api/v1/partidos")).bodyJson()
					.extractingPath("$.conteudo[*].sigla").asArray().containsExactly("PL", "PT");
		}

		@Test
		void deputadosDoPartido() {
			assertThat(get("/api/v1/partidos/37906/deputados")).bodyJson()
					.extractingPath("$.conteudo[*].nome").asArray().containsExactly("Ana Silva");
		}

		@Test
		void partidoInexistenteE404() {
			assertThat(get("/api/v1/partidos/1")).hasStatus(HttpStatus.NOT_FOUND);
			assertThat(get("/api/v1/partidos/1/deputados")).hasStatus(HttpStatus.NOT_FOUND);
		}
	}

	@Nested
	class Proposicoes {

		@Test
		void listagemPadraoDoMaisRecenteParaOMaisAntigo() {
			assertThat(get("/api/v1/proposicoes")).bodyJson()
					.extractingPath("$.conteudo[*].identificacao").asArray()
					.containsExactly("PL 200/2026", "PEC 7/2026", "PL 100/2025");
		}

		@Test
		void localizaPelaIdentificacaoQueOCidadaoUsa() {
			assertThat(get("/api/v1/proposicoes?tipo=pl&numero=100&ano=2025")).bodyJson()
					.extractingPath("$.conteudo[*].id").asArray().containsExactly(5001);
		}

		@Test
		void ementaIgnoraMaiusculasETrataCuringasComoTexto() {
			assertThat(get("/api/v1/proposicoes?ementa=SAÚDE")).bodyJson()
					.extractingPath("$.conteudo[*].id").asArray().containsExactly(5001);
			// "%" digitado é literal: só a ementa que contém "50%" deve voltar.
			assertThat(get("/api/v1/proposicoes?ementa=50%")).bodyJson()
					.extractingPath("$.conteudo[*].id").asArray().containsExactly(5002);
		}

		@Test
		void detalheEnriquecidoTrazSituacaoAtual() {
			var resposta = assertThat(get("/api/v1/proposicoes/5001")).hasStatusOk().bodyJson();

			resposta.extractingPath("$.identificacao").isEqualTo("PL 100/2025");
			resposta.extractingPath("$.detalheCarregado").isEqualTo(true);
			resposta.extractingPath("$.dataApresentacao").isEqualTo("2025-04-09T18:16:00");
			resposta.extractingPath("$.situacao.descricao").isEqualTo("Aguardando Parecer");
			resposta.extractingPath("$.situacao.orgaoSigla").isEqualTo("CCJC");
			// Quando a situação foi consultada, e não quando a Fase A regravou a ementa (_fonte).
			resposta.extractingPath("$.situacao.atualizadaEm").isEqualTo("2026-09-20T03:00:00");
		}

		@Test
		void listagemInformaQuandoASituacaoFoiConsultada() {
			var resposta = assertThat(get("/api/v1/proposicoes?tipo=PL&numero=100&ano=2025")).bodyJson();

			resposta.extractingPath("$.conteudo[0].situacaoAtualizadaEm").isEqualTo("2026-09-20T03:00:00");
			assertThat(get("/api/v1/proposicoes/5002")).bodyJson()
					.extractingPath("$").asMap().containsEntry("situacao", null);
		}

		@Test
		void procedenciaDaAutoriaCompletaEAConsultaDoDetalhe() {
			assertThat(get("/api/v1/proposicoes/5001/autores")).bodyJson()
					.extractingPath("$._fonte.atualizadoEm").isEqualTo("2026-09-20T03:00:00");
		}

		@Test
		void detalhePendenteExpoeDetalheCarregadoFalseEOsCamposComoNull() {
			var resposta = assertThat(get("/api/v1/proposicoes/5002")).hasStatusOk().bodyJson();

			resposta.extractingPath("$.detalheCarregado").isEqualTo(false);
			resposta.extractingPath("$").asMap()
					.containsEntry("situacao", null)
					.containsEntry("dataApresentacao", null)
					.containsEntry("urlInteiroTeor", null);
		}

		@Test
		void autoresNaOrdemDeAssinaturaComNaoDeputadoSemVinculo() {
			var resposta = assertThat(get("/api/v1/proposicoes/5001/autores")).hasStatusOk().bodyJson();

			resposta.extractingPath("$.autores[*].nome").asArray()
					.containsExactly("Ana Silva", "Poder Executivo", "Bruno Souza");
			resposta.extractingPath("$.autores[0].deputado.id").isEqualTo(1001);
			resposta.extractingPath("$.autores[0].proponente").isEqualTo(true);
			resposta.extractingPath("$.autores[1]").asMap().containsEntry("deputado", null);
			resposta.extractingPath("$.autores[1].tipo").isEqualTo("Órgão do Poder Executivo");
			resposta.extractingPath("$.autores[2].proponente").isEqualTo(false);
		}

		@Test
		void autoresDeProposicaoPendenteSemOrdemNemProponente() {
			var resposta = assertThat(get("/api/v1/proposicoes/5002/autores")).hasStatusOk().bodyJson();

			resposta.extractingPath("$.detalheCarregado").isEqualTo(false);
			resposta.extractingPath("$.autores[0]").asMap()
					.containsEntry("ordemAssinatura", null)
					.containsEntry("proponente", null);
		}
	}

	@Nested
	class Erros {

		@Test
		void naoEncontradoNoFormatoStandardError() {
			var resposta = assertThat(get("/api/v1/deputados/999999")).hasStatus(HttpStatus.NOT_FOUND).bodyJson();

			resposta.extractingPath("$.status").isEqualTo(404);
			resposta.extractingPath("$.erro").isEqualTo("Não encontrado");
			resposta.extractingPath("$.mensagem").isEqualTo("Deputado 999999 não encontrado");
			resposta.extractingPath("$.caminho").isEqualTo("/api/v1/deputados/999999");
			resposta.extractingPath("$.timestamp").isNotNull();
		}

		@Test
		void tipoErradoNoPathOuNaQueryE400() {
			assertThat(get("/api/v1/deputados/abc")).hasStatus(HttpStatus.BAD_REQUEST)
					.bodyJson().extractingPath("$.mensagem").asString().contains("'id'", "'abc'");
			assertThat(get("/api/v1/proposicoes?ano=abc")).hasStatus(HttpStatus.BAD_REQUEST)
					.bodyJson().extractingPath("$.mensagem").asString().contains("'ano'");
		}

		@Test
		void ordenacaoPorCampoInexistenteE400() {
			assertThat(get("/api/v1/deputados?sort=foo")).hasStatus(HttpStatus.BAD_REQUEST)
					.bodyJson().extractingPath("$.mensagem").asString().contains("'foo'");
		}

		@Test
		void limiteInvalidoNoEnriquecimentoE400() {
			assertThat(mvc.post().uri("/api/v1/admin/ingestao/enriquecimento?limite=0").header("X-Admin-Key", CHAVE_ADMIN))
					.hasStatus(HttpStatus.BAD_REQUEST)
					.bodyJson().extractingPath("$.mensagem").isEqualTo("limite: deve ser maior que zero");
		}

		@Test
		void rotaInexistenteE404EMetodoNaoSuportadoE405() {
			assertThat(get("/api/v1/nada")).hasStatus(HttpStatus.NOT_FOUND)
					.bodyJson().extractingPath("$.erro").isEqualTo("Não encontrado");
			assertThat(mvc.delete().uri("/api/v1/deputados")).hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
					.bodyJson().extractingPath("$.erro").isEqualTo("Método não permitido");
		}
	}

	@Nested
	class ExposicaoPublica {

		private static final String ENRIQUECIMENTO = "/api/v1/admin/ingestao/enriquecimento?limite=0";

		@Test
		void adminSemChaveE401() {
			assertThat(mvc.post().uri(ENRIQUECIMENTO)).hasStatus(HttpStatus.UNAUTHORIZED)
					.bodyJson().extractingPath("$.erro").isEqualTo("Não autorizado");
		}

		@Test
		void adminComChaveErradaE401() {
			assertThat(mvc.post().uri(ENRIQUECIMENTO).header("X-Admin-Key", "chave-errada"))
					.hasStatus(HttpStatus.UNAUTHORIZED);
		}

		@Test
		void endpointsPublicosNaoExigemChave() {
			assertThat(get("/api/v1/deputados")).hasStatusOk();
		}

		@Test
		void corsLiberaLeituraParaOFrontendConfigurado() {
			assertThat(mvc.options().uri("/api/v1/deputados")
					.header("Origin", "https://front.exemplo.org")
					.header("Access-Control-Request-Method", "GET"))
					.hasStatusOk()
					.hasHeader("Access-Control-Allow-Origin", "https://front.exemplo.org");
		}

		@Test
		void corsRecusaOutrasOrigensEEscritaPeloNavegador() {
			assertThat(mvc.options().uri("/api/v1/deputados")
					.header("Origin", "https://site-qualquer.com")
					.header("Access-Control-Request-Method", "GET"))
					.hasStatus(HttpStatus.FORBIDDEN);
			assertThat(mvc.options().uri("/api/v1/admin/ingestao/base")
					.header("Origin", "https://front.exemplo.org")
					.header("Access-Control-Request-Method", "POST"))
					.hasStatus(HttpStatus.FORBIDDEN);
		}

		@Test
		void healthCheckRespondeUpComOBanco() {
			assertThat(get("/actuator/health")).hasStatusOk()
					.bodyJson().extractingPath("$.status").isEqualTo("UP");
		}

		@Test
		void actuatorNaoExpoeOutrosEndpoints() {
			assertThat(get("/actuator/env")).hasStatus(HttpStatus.NOT_FOUND);
			assertThat(get("/actuator/beans")).hasStatus(HttpStatus.NOT_FOUND);
		}
	}

	@Nested
	class Documentacao {

		@Test
		void openApiExplicaOrigemRecorteDetalheCarregadoETotalProposicoes() {
			var descricao = assertThat(get("/v3/api-docs")).hasStatusOk().bodyJson()
					.extractingPath("$.info.description").asString();

			descricao.contains("Dados Abertos da Câmara dos Deputados");
			descricao.contains("Legislatura: **57**", "2023-02-01", "PL, PEC, PLP, PDL");
			descricao.contains("`detalheCarregado`");
			descricao.contains("Não é métrica de produtividade parlamentar");
		}

		@Test
		void paginacaoApareceComoPageSizeESort() {
			assertThat(get("/v3/api-docs")).bodyJson()
					.extractingPath("$.paths['/api/v1/deputados'].get.parameters[*].name").asArray()
					.contains("uf", "partido", "nome", "page", "size", "sort")
					.doesNotContain("pageable");
		}

		@Test
		void swaggerUiDisponivel() {
			assertThat(get("/swagger-ui.html")).hasStatus3xxRedirection();
		}
	}

	private MvcTestResult get(String uri) {
		return mvc.get().uri(uri).exchange();
	}
}
