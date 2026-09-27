package com.example.parlamento.ingestao.mapper;

import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.entity.Proposicao;
import com.example.parlamento.domain.entity.ProposicaoAutor;
import com.example.parlamento.ingestao.dto.AutorCamaraDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutorMapperTest {

	private static final String URI_DEPUTADO = "https://dadosabertos.camara.leg.br/api/v2/deputados/204531";

	@Test
	void deputadoComUriResolveOId() {
		AutorCamaraDto autor = new AutorCamaraDto(URI_DEPUTADO, "Guilherme Derrite", 10000, "Deputado(a)", 1, 1);

		assertThat(AutorMapper.ehDeputado(autor)).isTrue();
		assertThat(AutorMapper.idDeputado(autor)).contains(204531L);
	}

	@Test
	void naoDeputadoSemUriNaoResolveId() {
		AutorCamaraDto executivo = new AutorCamaraDto(null, "Poder Executivo", 40000, "Órgão do Poder Executivo", 1, 1);

		assertThat(AutorMapper.ehDeputado(executivo)).isFalse();
		assertThat(AutorMapper.idDeputado(executivo)).isEmpty();
	}

	@Test
	void naoDeputadoComUriNumericaNaoEhConfundidoComDeputado() {
		// Uma comissão tem URI de órgão terminando em número: não pode virar deputado_id.
		AutorCamaraDto comissao = new AutorCamaraDto("https://dadosabertos.camara.leg.br/api/v2/orgaos/2003",
				"Comissão de Saúde", 2, "Comissão Permanente", 1, 1);

		assertThat(AutorMapper.idDeputado(comissao)).isEmpty();
	}

	@Test
	void deputadoSemUriNaoResolveId() {
		AutorCamaraDto autor = new AutorCamaraDto(null, "Deputado Sem Uri", 10000, "Deputado(a)", 2, 0);

		assertThat(AutorMapper.ehDeputado(autor)).isTrue();
		assertThat(AutorMapper.idDeputado(autor)).isEmpty();
	}

	@Test
	void semCodTipoUsaOTextoDoTipo() {
		assertThat(AutorMapper.ehDeputado(new AutorCamaraDto(URI_DEPUTADO, "X", null, "Deputado(a)", null, null))).isTrue();
		assertThat(AutorMapper.ehDeputado(new AutorCamaraDto(null, "Senado", null, "Senado Federal", null, null))).isFalse();
		assertThat(AutorMapper.ehDeputado(new AutorCamaraDto(null, "?", null, null, null, null))).isFalse();
	}

	@Test
	void paraEntidadeCopiaTodosOsCamposDaFonte() {
		Proposicao proposicao = new Proposicao(2277677L);
		Deputado deputado = new Deputado(204531L);
		AutorCamaraDto dto = new AutorCamaraDto(URI_DEPUTADO, "Guilherme Derrite", 10000, "Deputado(a)", 1, 1);

		ProposicaoAutor autor = AutorMapper.paraEntidade(proposicao, dto, deputado);

		assertThat(autor.getProposicao()).isSameAs(proposicao);
		assertThat(autor.getDeputado()).isSameAs(deputado);
		assertThat(autor.getNome()).isEqualTo("Guilherme Derrite");
		assertThat(autor.getTipo()).isEqualTo("Deputado(a)");
		assertThat(autor.getCodTipo()).isEqualTo(10000);
		assertThat(autor.getOrdemAssinatura()).isEqualTo(1);
		assertThat(autor.getProponente()).isEqualTo(1);
	}

	@Test
	void paraEntidadeAceitaAutorSemDeputado() {
		AutorCamaraDto executivo = new AutorCamaraDto(null, "Poder Executivo", 40000, "Órgão do Poder Executivo", 1, 1);

		ProposicaoAutor autor = AutorMapper.paraEntidade(new Proposicao(1L), executivo, null);

		assertThat(autor.getDeputado()).isNull();
		assertThat(autor.getNome()).isEqualTo("Poder Executivo");
	}
}
