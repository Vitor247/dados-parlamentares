package com.example.parlamento.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/** Metadados da documentação OpenAPI (Swagger UI em {@code /swagger-ui.html}). */
@Configuration
public class OpenApiConfig {

	/** Nome do esquema de segurança referenciado pelos endpoints de administração. */
	public static final String ESQUEMA_ADMIN = "chaveAdmin";

	@Bean
	OpenAPI openApi(IngestaoProperties recorte) {
		return new OpenAPI()
				.components(new Components().addSecuritySchemes(ESQUEMA_ADMIN, new SecurityScheme()
						.type(SecurityScheme.Type.APIKEY)
						.in(SecurityScheme.In.HEADER)
						.name(AdminApiKeyInterceptor.HEADER)
						.description("Chave de administração (variável ADMIN_API_KEY do servidor)")))
				.info(new Info()
						.title("Plataforma de Dados Parlamentares")
						.version("v1")
						.description(descricao(recorte))
						.license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
				.tags(List.of(
						new Tag().name("Deputados").description("Deputados federais, com partido atual e proposições de autoria"),
						new Tag().name("Partidos").description("Partidos com representação na legislatura importada"),
						new Tag().name("Proposições").description("Proposições legislativas, situação atual e autoria"),
						new Tag().name("Administração").description("Disparo manual da ingestão. Exige o header "
								+ AdminApiKeyInterceptor.HEADER + " (botão Authorize)")));
	}

	/** O recorte vem da configuração: a documentação acompanha o que de fato foi importado. */
	private static String descricao(IngestaoProperties recorte) {
		return """
				API pública e somente leitura sobre deputados federais, partidos e proposições.

				## Origem dos dados
				Todos os dados vêm da **API de Dados Abertos da Câmara dos Deputados**
				(https://dadosabertos.camara.leg.br), são importados para uma base própria e servidos aqui.
				Nenhum dado é inventado, inferido ou completado. Todo recurso traz um bloco `_fonte` com a
				origem, a URI oficial na Câmara e o momento da última atualização na nossa base.

				Esta API **não é ferramenta de avaliação política**: não há ranking, nota ou classificação.

				## Recorte de dados vigente
				- Legislatura: **%d**
				- Proposições apresentadas a partir de **%s**
				- Tipos de proposição: **%s**

				Proposições fora desse recorte não estão na base, mesmo que existam na Câmara.

				## `detalheCarregado`
				A importação de proposições acontece em duas fases. Na primeira, só a identificação e a
				ementa são carregadas (`detalheCarregado = false`): situação, data de apresentação, inteiro
				teor e autoria completa (coautores, autores que não são deputados, ordem de assinatura)
				ainda **não foram carregados** e aparecem como `null`. Depois do enriquecimento,
				`detalheCarregado = true`.

				## Atualidade da situação
				A situação de uma proposição muda na Câmara ao longo da tramitação. Aqui ela é uma
				**fotografia**: o campo `situacao.atualizadaEm` (ou `situacaoAtualizadaEm` nas listagens)
				diz quando ela foi consultada na fonte, e ela é revalidada periodicamente (a cada %s).
				Não confunda com `_fonte.atualizadoEm`, que indica a última sincronização da identificação
				e da ementa.

				## `totalProposicoes`
				Contagem simples das proposições de autoria do deputado **que estão na nossa base**,
				dentro do recorte acima. **Não é métrica de produtividade parlamentar** e não deve ser usada
				para comparar deputados.

				## Convenções
				- Paginação: `page` começa em **0**; `size` padrão 20, máximo 100.
				- Campos que a fonte não publica vêm como `null` — nunca são omitidos nem preenchidos com zero.
				- Erros seguem o formato `StandardError` (`timestamp`, `status`, `erro`, `mensagem`, `caminho`).
				""".formatted(recorte.legislatura(), recorte.dataApresentacaoInicio(),
				String.join(", ", recorte.tiposProposicao()), descreverDuracao(recorte.validadeSituacao()));
	}

	private static String descreverDuracao(Duration duracao) {
		long dias = duracao.toDays();
		return dias >= 1 ? dias + (dias == 1 ? " dia" : " dias") : duracao.toHours() + " horas";
	}
}
