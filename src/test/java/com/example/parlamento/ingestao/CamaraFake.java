package com.example.parlamento.ingestao;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Monta stubs da API da Câmara no WireMock com o formato real dos payloads
 * (envelope {@code dados}/{@code links}, seção 6 da especificação).
 */
final class CamaraFake {

	static final String BASE = "/api/v2";
	private static final String URI = "https://dadosabertos.camara.leg.br/api/v2";

	private final WireMockServer wm;

	CamaraFake(WireMockServer wm) {
		this.wm = wm;
	}

	// ---------- stubs ----------

	void partidos(String... partidos) {
		wm.stubFor(get(urlPathEqualTo(BASE + "/partidos")).withQueryParam("pagina", equalTo("1"))
				.willReturn(okJson(envelope(lista(partidos), false))));
	}

	/** Uma página da lista de deputados; {@code temProxima} adiciona o link "next". */
	void deputadosPagina(int pagina, boolean temProxima, String... deputados) {
		wm.stubFor(get(urlPathEqualTo(BASE + "/deputados")).withQueryParam("pagina", equalTo(String.valueOf(pagina)))
				.willReturn(okJson(envelope(lista(deputados), temProxima))));
	}

	void detalheDeputado(long id, String siglaPartidoAtual) {
		wm.stubFor(get(urlPathEqualTo(BASE + "/deputados/" + id))
				.willReturn(okJson(envelope(detalheDeputadoJson(id, siglaPartidoAtual), false))));
	}

	/** Resposta padrão (baixa prioridade): deputado sem proposições no recorte. */
	void semProposicoesPorPadrao() {
		wm.stubFor(get(urlPathEqualTo(BASE + "/proposicoes")).atPriority(10)
				.willReturn(okJson(envelope("[]", false))));
	}

	void proposicoesDoAutor(long idDeputado, String... proposicoes) {
		wm.stubFor(get(urlPathEqualTo(BASE + "/proposicoes"))
				.withQueryParam("idDeputadoAutor", equalTo(String.valueOf(idDeputado)))
				.withQueryParam("pagina", equalTo("1"))
				.atPriority(1)
				.willReturn(okJson(envelope(lista(proposicoes), false))));
	}

	void detalheProposicao(long id) {
		wm.stubFor(get(urlPathEqualTo(BASE + "/proposicoes/" + id))
				.willReturn(okJson(envelope(detalheProposicaoJson(id), false))));
	}

	void autores(long idProposicao, String... autores) {
		wm.stubFor(get(urlPathEqualTo(BASE + "/proposicoes/" + idProposicao + "/autores"))
				.willReturn(okJson(envelope(lista(autores), false))));
	}

	// ---------- payloads ----------

	static String partido(long id, String sigla) {
		return """
				{"id":%d,"sigla":"%s","nome":"Partido %s","uri":"%s/partidos/%d"}"""
				.formatted(id, sigla, sigla, URI, id);
	}

	/** Item da lista: é daqui que vem o vínculo com o partido. */
	static String deputado(long id, String nome, String sigla, long idPartido) {
		return """
				{"id":%d,"uri":"%s/deputados/%d","nome":"%s","siglaPartido":"%s",
				 "uriPartido":"%s/partidos/%d","siglaUf":"MG","idLegislatura":57,
				 "urlFoto":null,"email":null}"""
				.formatted(id, URI, id, nome, sigla, URI, idPartido);
	}

	static String proposicao(long id, String siglaTipo, int numero, int ano) {
		return """
				{"id":%d,"uri":"%s/proposicoes/%d","siglaTipo":"%s","codTipo":139,
				 "numero":%d,"ano":%d,"ementa":"Ementa da proposição %d"}"""
				.formatted(id, URI, id, siglaTipo, numero, ano, id);
	}

	static String autorDeputado(long idDeputado, String nome, int ordem) {
		return """
				{"uri":"%s/deputados/%d","nome":"%s","codTipo":10000,"tipo":"Deputado(a)",
				 "ordemAssinatura":%d,"proponente":1}"""
				.formatted(URI, idDeputado, nome, ordem);
	}

	/** Autor que não é deputado: sem URI, com outro tipo (caso real da seção 6.6). */
	static String autorExterno(String nome, String tipo, int ordem) {
		return """
				{"uri":null,"nome":"%s","codTipo":40000,"tipo":"%s","ordemAssinatura":%d,"proponente":1}"""
				.formatted(nome, tipo, ordem);
	}

	/** Detalhe real: {@code ultimoStatus.uriPartido} vem null mesmo com a sigla preenchida. */
	private static String detalheDeputadoJson(long id, String siglaPartidoAtual) {
		return """
				{"id":%d,"uri":"%s/deputados/%d","nomeCivil":"NOME CIVIL %d",
				 "ultimoStatus":{"nome":"Deputado %d","siglaPartido":"%s","uriPartido":null,"siglaUf":"MG",
				   "idLegislatura":57,"urlFoto":null,"email":null,"situacao":"Exercício","condicaoEleitoral":"Titular"},
				 "cpf":"00000000000","sexo":"M","dataNascimento":"1976-03-03","dataFalecimento":null,
				 "ufNascimento":"MG","municipioNascimento":"Belo Horizonte","escolaridade":"Superior"}"""
				.formatted(id, URI, id, id, id, siglaPartidoAtual);
	}

	private static String detalheProposicaoJson(long id) {
		return """
				{"id":%d,"uri":"%s/proposicoes/%d","siglaTipo":"PL","codTipo":139,"numero":%d,"ano":2025,
				 "ementa":"Ementa detalhada","dataApresentacao":"2025-04-09T18:16","descricaoTipo":"Projeto de Lei",
				 "urlInteiroTeor":"https://www.camara.leg.br/teor/%d",
				 "statusProposicao":{"dataHora":"2025-04-27T00:00","siglaOrgao":"CCJC","descricaoTramitacao":"Recebimento",
				   "descricaoSituacao":"Aguardando Parecer","codSituacao":1100,"despacho":"","ambito":"Regimental"}}"""
				.formatted(id, URI, id, id % 10000, id);
	}

	private static String lista(String... itens) {
		return Arrays.stream(itens).collect(Collectors.joining(",", "[", "]"));
	}

	private static String envelope(String dados, boolean temProxima) {
		String proxima = temProxima ? ",{\"rel\":\"next\",\"href\":\"" + URI + "/proxima\"}" : "";
		return "{\"dados\":" + dados + ",\"links\":[{\"rel\":\"self\",\"href\":\"" + URI + "\"}" + proxima + "]}";
	}
}
