package com.example.parlamento.ingestao.client;

import com.example.parlamento.config.CamaraProperties;
import com.example.parlamento.exception.FonteExternaIndisponivelException;
import com.example.parlamento.ingestao.dto.AutorCamaraDto;
import com.example.parlamento.ingestao.dto.DeputadoDetalheCamaraDto;
import com.example.parlamento.ingestao.dto.DeputadoResumoCamaraDto;
import com.example.parlamento.ingestao.dto.PartidoCamaraDto;
import com.example.parlamento.ingestao.dto.ProposicaoDetalheCamaraDto;
import com.example.parlamento.ingestao.dto.ProposicaoResumoCamaraDto;
import com.example.parlamento.ingestao.dto.RespostaCamara;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Acesso à API de Dados Abertos da Câmara dos Deputados.
 * Toda chamada passa pelo retry (429, 5xx e I/O); listas são percorridas página a página.
 */
@Component
public class CamaraClient {

	/** Trava de segurança contra loop de paginação caso a fonte sempre anuncie "next". */
	private static final int LIMITE_PAGINAS = 1_000;

	private static final ParameterizedTypeReference<RespostaCamara<List<PartidoCamaraDto>>> LISTA_PARTIDOS =
			new ParameterizedTypeReference<>() { };
	private static final ParameterizedTypeReference<RespostaCamara<List<DeputadoResumoCamaraDto>>> LISTA_DEPUTADOS =
			new ParameterizedTypeReference<>() { };
	private static final ParameterizedTypeReference<RespostaCamara<DeputadoDetalheCamaraDto>> DETALHE_DEPUTADO =
			new ParameterizedTypeReference<>() { };
	private static final ParameterizedTypeReference<RespostaCamara<List<ProposicaoResumoCamaraDto>>> LISTA_PROPOSICOES =
			new ParameterizedTypeReference<>() { };
	private static final ParameterizedTypeReference<RespostaCamara<ProposicaoDetalheCamaraDto>> DETALHE_PROPOSICAO =
			new ParameterizedTypeReference<>() { };
	private static final ParameterizedTypeReference<RespostaCamara<List<AutorCamaraDto>>> LISTA_AUTORES =
			new ParameterizedTypeReference<>() { };

	private final RestClient restClient;
	private final RetryTemplate retryTemplate;
	private final int itensPorPagina;

	public CamaraClient(RestClient camaraRestClient, RetryTemplate camaraRetryTemplate, CamaraProperties props) {
		this.restClient = camaraRestClient;
		this.retryTemplate = camaraRetryTemplate;
		this.itensPorPagina = props.itensPorPagina();
	}

	/**
	 * Partidos com representação na legislatura. Sem o filtro, a fonte devolve só os
	 * partidos atuais e omite os extintos/incorporados que ainda aparecem na legislatura.
	 */
	public List<PartidoCamaraDto> listarPartidos(int idLegislatura) {
		return listarTodasPaginas("/partidos", uri -> uri
				.queryParam("idLegislatura", idLegislatura)
				.queryParam("ordem", "ASC")
				.queryParam("ordenarPor", "id"), LISTA_PARTIDOS);
	}

	public List<DeputadoResumoCamaraDto> listarDeputados(int idLegislatura) {
		return listarTodasPaginas("/deputados", uri -> uri
				.queryParam("idLegislatura", idLegislatura)
				.queryParam("ordem", "ASC")
				.queryParam("ordenarPor", "id"), LISTA_DEPUTADOS);
	}

	public Optional<DeputadoDetalheCamaraDto> buscarDeputado(long id) {
		return buscarUm("/deputados/{id}", id, DETALHE_DEPUTADO);
	}

	/**
	 * Proposições de autoria de um deputado, apresentadas a partir de {@code dataApresentacaoInicio}.
	 * Obs.: o parâmetro {@code dataInicio} da fonte filtra por data de <em>tramitação</em>,
	 * não de apresentação, e traria proposições antigas que apenas tramitaram no período.
	 */
	public List<ProposicaoResumoCamaraDto> listarProposicoesPorAutor(
			long idDeputado, LocalDate dataApresentacaoInicio, List<String> siglasTipo) {
		return listarTodasPaginas("/proposicoes", uri -> {
			uri.queryParam("idDeputadoAutor", idDeputado)
					.queryParam("dataApresentacaoInicio", dataApresentacaoInicio)
					.queryParam("ordem", "ASC")
					.queryParam("ordenarPor", "id");
			if (siglasTipo != null && !siglasTipo.isEmpty()) {
				uri.queryParam("siglaTipo", String.join(",", siglasTipo));
			}
		}, LISTA_PROPOSICOES);
	}

	public Optional<ProposicaoDetalheCamaraDto> buscarProposicao(long id) {
		return buscarUm("/proposicoes/{id}", id, DETALHE_PROPOSICAO);
	}

	/** Endpoint não paginado: devolve todos os autores de uma vez. */
	public List<AutorCamaraDto> listarAutores(long idProposicao) {
		RespostaCamara<List<AutorCamaraDto>> resposta = executar("autores da proposição " + idProposicao,
				() -> restClient.get()
						.uri("/proposicoes/{id}/autores", idProposicao)
						.retrieve()
						.body(LISTA_AUTORES));
		return resposta == null || resposta.dados() == null ? List.of() : resposta.dados();
	}

	/** Só vale a pena repetir o que é transitório: limite de taxa, erro do servidor, rede. */
	public static boolean deveRetentar(Throwable erro) {
		return erro instanceof HttpClientErrorException.TooManyRequests
				|| erro instanceof HttpServerErrorException
				|| erro instanceof ResourceAccessException;
	}

	private <T> List<T> listarTodasPaginas(String caminho, Consumer<UriBuilder> parametros,
			ParameterizedTypeReference<RespostaCamara<List<T>>> tipo) {
		List<T> todos = new ArrayList<>();
		for (int pagina = 1; pagina <= LIMITE_PAGINAS; pagina++) {
			int paginaAtual = pagina;
			RespostaCamara<List<T>> resposta = executar(caminho + " página " + paginaAtual,
					() -> restClient.get()
							.uri(uri -> {
								uri.path(caminho);
								parametros.accept(uri);
								return uri.queryParam("pagina", paginaAtual)
										.queryParam("itens", itensPorPagina)
										.build();
							})
							.retrieve()
							.body(tipo));

			if (resposta == null || resposta.dados() == null || resposta.dados().isEmpty()) {
				break;
			}
			todos.addAll(resposta.dados());
			if (!resposta.temProximaPagina()) {
				break;
			}
		}
		return todos;
	}

	private <T> Optional<T> buscarUm(String caminho, long id,
			ParameterizedTypeReference<RespostaCamara<T>> tipo) {
		try {
			RespostaCamara<T> resposta = executar(caminho.replace("{id}", String.valueOf(id)),
					() -> restClient.get().uri(caminho, id).retrieve().body(tipo));
			return Optional.ofNullable(resposta).map(RespostaCamara::dados);
		} catch (HttpClientErrorException.NotFound e) {
			return Optional.empty();
		}
	}

	private <R> R executar(String descricao, Supplier<R> chamada) {
		try {
			return retryTemplate.execute(new Retryable<R>() {
				@Override
				public R execute() {
					return chamada.get();
				}

				@Override
				public String getName() {
					return descricao;
				}
			});
		} catch (RetryException e) {
			// 404 não é indisponibilidade: quem chamou decide o que fazer com ele.
			if (e.getLastException() instanceof HttpClientErrorException.NotFound notFound) {
				throw notFound;
			}
			throw new FonteExternaIndisponivelException(
					"Falha ao consultar " + descricao + " na API da Câmara", e.getLastException());
		}
	}
}
