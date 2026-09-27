package com.example.parlamento.ingestao.mapper;

import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.entity.Partido;
import com.example.parlamento.ingestao.dto.DeputadoDetalheCamaraDto;
import com.example.parlamento.ingestao.dto.DeputadoResumoCamaraDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeputadoMapperTest {

	private static final String URI_PARTIDOS = "https://dadosabertos.camara.leg.br/api/v2/partidos/";

	@Nested
	class RegistroAtual {

		// A lista da legislatura repete o deputado uma vez por partido: UNIÃO → PL.
		private final DeputadoResumoCamaraDto uniao = resumo("UNIÃO", 38009L);
		private final DeputadoResumoCamaraDto pl = resumo("PL", 37906L);

		@Test
		void escolheORegistroDoPartidoAtualInformadoNoDetalhe() {
			assertThat(DeputadoMapper.registroAtual(List.of(pl, uniao), detalhe("PL")))
					.isSameAs(pl);
		}

		@Test
		void mesmaSiglaRepetidaFicaComAUltimaOcorrencia() {
			DeputadoResumoCamaraDto plAntigo = resumo("PL", 37906L);
			assertThat(DeputadoMapper.registroAtual(List.of(plAntigo, uniao, pl), detalhe("PL")))
					.isSameAs(pl);
		}

		@Test
		void semDetalheFicaComOUltimoRegistroDaLista() {
			assertThat(DeputadoMapper.registroAtual(List.of(uniao, pl), null)).isSameAs(pl);
		}

		@Test
		void detalheSemUltimoStatusFicaComOUltimoRegistro() {
			DeputadoDetalheCamaraDto semStatus = new DeputadoDetalheCamaraDto(
					1L, null, "NOME CIVIL", null, null, null, null, null, null);
			assertThat(DeputadoMapper.registroAtual(List.of(uniao, pl), semStatus)).isSameAs(pl);
		}

		@Test
		void siglaDoDetalheSemCorrespondenciaNaListaFicaComOUltimoRegistro() {
			assertThat(DeputadoMapper.registroAtual(List.of(uniao, pl), detalhe("PSD"))).isSameAs(pl);
		}

		@Test
		void listaVaziaEhErro() {
			assertThatThrownBy(() -> DeputadoMapper.registroAtual(List.of(), detalhe("PL")))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	class AtualizarDoDetalhe {

		@Test
		void naoTocaNoPartidoPorqueUriPartidoVemNullNoDetalhe() {
			Partido pl = new Partido(37906L);
			Deputado deputado = new Deputado(92776L);
			DeputadoMapper.atualizarDoResumo(deputado, resumo("PL", 37906L), pl, LocalDateTime.now());

			// Detalhe real: siglaPartido preenchida, uriPartido null.
			DeputadoMapper.atualizarDoDetalhe(deputado, detalhe("PSD"));

			assertThat(deputado.getPartido()).isSameAs(pl);
			assertThat(deputado.getSiglaPartido()).isEqualTo("PL");
		}

		@Test
		void enriqueceDadosPessoaisESituacao() {
			Deputado deputado = new Deputado(92776L);
			DeputadoMapper.atualizarDoDetalhe(deputado, detalhe("PSD"));

			assertThat(deputado.getNomeCivil()).isEqualTo("STEFANO AGUIAR DOS SANTOS");
			assertThat(deputado.getDataNascimento()).isEqualTo(LocalDate.of(1976, 3, 3));
			assertThat(deputado.getMunicipioNascimento()).isEqualTo("Belo Horizonte");
			assertThat(deputado.getSituacao()).isEqualTo("Exercício");
			assertThat(deputado.getCondicaoEleitoral()).isEqualTo("Titular");
		}

		@Test
		void dataDeNascimentoMalformadaViraNullSemDerrubarORegistro() {
			Deputado deputado = new Deputado(1L);
			DeputadoMapper.atualizarDoDetalhe(deputado, new DeputadoDetalheCamaraDto(
					1L, null, "FULANO", null, "03/03/1976", null, null, null, null));

			assertThat(deputado.getNomeCivil()).isEqualTo("FULANO");
			assertThat(deputado.getDataNascimento()).isNull();
		}

		@Test
		void semUltimoStatusMantemSituacaoAnterior() {
			Deputado deputado = new Deputado(1L);
			deputado.setSituacao("Exercício");
			DeputadoMapper.atualizarDoDetalhe(deputado, new DeputadoDetalheCamaraDto(
					1L, null, "FULANO", null, null, null, null, null, null));

			assertThat(deputado.getSituacao()).isEqualTo("Exercício");
		}
	}

	private static DeputadoResumoCamaraDto resumo(String siglaPartido, Long idPartido) {
		return new DeputadoResumoCamaraDto(92776L, null, "Stefano Aguiar", siglaPartido,
				URI_PARTIDOS + idPartido, "MG", 57, null, null);
	}

	private static DeputadoDetalheCamaraDto detalhe(String siglaPartidoAtual) {
		var status = new DeputadoDetalheCamaraDto.UltimoStatus("Stefano Aguiar", siglaPartidoAtual, null,
				"MG", 57, null, null, "Exercício", "Titular");
		return new DeputadoDetalheCamaraDto(92776L, null, "STEFANO AGUIAR DOS SANTOS", status,
				"1976-03-03", null, "MG", "Belo Horizonte", "Superior");
	}
}
