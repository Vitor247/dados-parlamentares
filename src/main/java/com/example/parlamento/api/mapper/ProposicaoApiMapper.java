package com.example.parlamento.api.mapper;

import com.example.parlamento.api.dto.AutoresDto;
import com.example.parlamento.api.dto.FonteDto;
import com.example.parlamento.api.dto.ProposicaoDto;
import com.example.parlamento.api.dto.ProposicaoResumoDto;
import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.entity.Proposicao;
import com.example.parlamento.domain.entity.ProposicaoAutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ProposicaoApiMapper {

	private final FonteMapper fonteMapper;

	public ProposicaoApiMapper(FonteMapper fonteMapper) {
		this.fonteMapper = fonteMapper;
	}

	public ProposicaoResumoDto paraResumo(Proposicao proposicao) {
		return new ProposicaoResumoDto(
				proposicao.getId(),
				identificacao(proposicao),
				proposicao.getSiglaTipo(),
				proposicao.getNumero(),
				proposicao.getAno(),
				proposicao.getEmenta(),
				proposicao.getDataApresentacao(),
				proposicao.getSituacaoDescricao(),
				proposicao.getDetalheAtualizadoEm(),
				proposicao.isDetalheCarregado(),
				fonte(proposicao));
	}

	public ProposicaoDto paraDto(Proposicao proposicao) {
		ProposicaoDto.SituacaoDto situacao = proposicao.isDetalheCarregado()
				? new ProposicaoDto.SituacaoDto(
						proposicao.getSituacaoDescricao(),
						proposicao.getSituacaoCod(),
						proposicao.getSituacaoData(),
						proposicao.getSituacaoOrgaoSigla(),
						proposicao.getTramitacaoDescricao(),
						proposicao.getDetalheAtualizadoEm())
				: null;
		return new ProposicaoDto(
				proposicao.getId(),
				identificacao(proposicao),
				proposicao.getSiglaTipo(),
				proposicao.getCodTipo(),
				proposicao.getDescricaoTipo(),
				proposicao.getNumero(),
				proposicao.getAno(),
				proposicao.getEmenta(),
				proposicao.getDataApresentacao(),
				proposicao.getUrlInteiroTeor(),
				situacao,
				proposicao.isDetalheCarregado(),
				fonte(proposicao));
	}

	/**
	 * A autoria completa vem do enriquecimento: quando carregada, a procedência informa
	 * quando ela foi buscada, e não a última regravação da identificação pela Fase A.
	 */
	public AutoresDto paraAutores(Proposicao proposicao, List<ProposicaoAutor> autores) {
		LocalDateTime autoriaAtualizadaEm = proposicao.isDetalheCarregado()
				? proposicao.getDetalheAtualizadoEm()
				: proposicao.getAtualizadoEm();
		return new AutoresDto(
				proposicao.getId(),
				identificacao(proposicao),
				proposicao.isDetalheCarregado(),
				autores.stream().map(ProposicaoApiMapper::paraAutor).toList(),
				fonteMapper.camara("proposicoes", proposicao.getId(), null, autoriaAtualizadaEm));
	}

	/** "PL 1234/2025" — o identificador que as pessoas de fato usam. */
	public static String identificacao(Proposicao proposicao) {
		return proposicao.getSiglaTipo() + " " + proposicao.getNumero() + "/" + proposicao.getAno();
	}

	private static AutoresDto.AutorDto paraAutor(ProposicaoAutor autor) {
		Deputado deputado = autor.getDeputado();
		AutoresDto.DeputadoRefDto ref = deputado == null
				? null
				: new AutoresDto.DeputadoRefDto(deputado.getId(), deputado.getNome(),
						deputado.getSiglaPartido(), deputado.getSiglaUf());
		// A fonte publica 1/0; ausência (ainda não carregada) continua null.
		Boolean proponente = autor.getProponente() == null ? null : autor.getProponente() == 1;
		return new AutoresDto.AutorDto(autor.getNome(), autor.getTipo(), autor.getOrdemAssinatura(), proponente, ref);
	}

	private FonteDto fonte(Proposicao proposicao) {
		return fonteMapper.camara("proposicoes", proposicao.getId(), null, proposicao.getAtualizadoEm());
	}
}
