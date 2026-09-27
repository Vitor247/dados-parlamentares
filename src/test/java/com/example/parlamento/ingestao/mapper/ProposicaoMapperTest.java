package com.example.parlamento.ingestao.mapper;

import com.example.parlamento.domain.entity.Proposicao;
import com.example.parlamento.ingestao.dto.ProposicaoDetalheCamaraDto;
import com.example.parlamento.ingestao.dto.ProposicaoResumoCamaraDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProposicaoMapperTest {

	private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 27, 18, 0);

	@Test
	void resumoNovoFicaComDetalheNaoCarregado() {
		Proposicao proposicao = new Proposicao(2277677L);

		ProposicaoMapper.atualizarDoResumo(proposicao, resumo(), AGORA);

		assertThat(proposicao.getSiglaTipo()).isEqualTo("PL");
		assertThat(proposicao.getNumero()).isEqualTo(16);
		assertThat(proposicao.getAno()).isEqualTo(2025);
		assertThat(proposicao.isDetalheCarregado()).isFalse();
		assertThat(proposicao.getAtualizadoEm()).isEqualTo(AGORA);
	}

	@Test
	void reimportarOResumoNaoDesfazOEnriquecimento() {
		Proposicao proposicao = new Proposicao(2277677L);
		ProposicaoMapper.atualizarDoDetalhe(proposicao, detalhe(status()), AGORA);

		ProposicaoMapper.atualizarDoResumo(proposicao, resumo(), AGORA.plusDays(1));

		assertThat(proposicao.isDetalheCarregado()).isTrue();
		assertThat(proposicao.getSituacaoDescricao()).isEqualTo("Arquivada");
		assertThat(proposicao.getDataApresentacao()).isEqualTo(LocalDateTime.of(2025, 4, 9, 18, 16));
	}

	@Test
	void detalheAchataAStatusEmColunas() {
		Proposicao proposicao = new Proposicao(2277677L);

		ProposicaoMapper.atualizarDoDetalhe(proposicao, detalhe(status()), AGORA);

		assertThat(proposicao.isDetalheCarregado()).isTrue();
		assertThat(proposicao.getDescricaoTipo()).isEqualTo("Projeto de Lei");
		assertThat(proposicao.getUrlInteiroTeor()).isEqualTo("https://www.camara.leg.br/teor");
		assertThat(proposicao.getSituacaoDescricao()).isEqualTo("Arquivada");
		assertThat(proposicao.getSituacaoCod()).isEqualTo(923);
		assertThat(proposicao.getSituacaoData()).isEqualTo(LocalDateTime.of(2025, 4, 27, 0, 0));
		assertThat(proposicao.getSituacaoOrgaoSigla()).isEqualTo("CSPCCO");
		assertThat(proposicao.getTramitacaoDescricao()).isEqualTo("Arquivamento");
	}

	@Test
	void detalheSemStatusMarcaComoCarregadoESituacaoFicaNull() {
		Proposicao proposicao = new Proposicao(2277677L);

		ProposicaoMapper.atualizarDoDetalhe(proposicao, detalhe(null), AGORA);

		assertThat(proposicao.isDetalheCarregado()).isTrue();
		assertThat(proposicao.getSituacaoDescricao()).isNull();
		assertThat(proposicao.getSituacaoCod()).isNull();
		assertThat(proposicao.getSituacaoData()).isNull();
	}

	private static ProposicaoResumoCamaraDto resumo() {
		return new ProposicaoResumoCamaraDto(2277677L, null, "PL", 139, 16, 2025, "Ementa da lista");
	}

	private static ProposicaoDetalheCamaraDto.StatusProposicao status() {
		return new ProposicaoDetalheCamaraDto.StatusProposicao("2025-04-27T00:00", "CSPCCO", "Arquivamento",
				"Arquivada", 923, "Arquivada", "Regimental");
	}

	private static ProposicaoDetalheCamaraDto detalhe(ProposicaoDetalheCamaraDto.StatusProposicao status) {
		return new ProposicaoDetalheCamaraDto(2277677L, null, "PL", 139, 16, 2025, "Ementa do detalhe",
				"2025-04-09T18:16", "Projeto de Lei", "https://www.camara.leg.br/teor", status);
	}
}
