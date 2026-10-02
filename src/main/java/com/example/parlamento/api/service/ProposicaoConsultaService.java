package com.example.parlamento.api.service;

import com.example.parlamento.api.dto.AutoresDto;
import com.example.parlamento.api.dto.PaginaDto;
import com.example.parlamento.api.dto.ProposicaoDto;
import com.example.parlamento.api.dto.ProposicaoResumoDto;
import com.example.parlamento.api.dto.SituacoesDto;
import com.example.parlamento.api.mapper.ProposicaoApiMapper;
import com.example.parlamento.domain.entity.Proposicao;
import com.example.parlamento.domain.repository.ProposicaoAutorRepository;
import com.example.parlamento.domain.repository.ProposicaoRepository;
import com.example.parlamento.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.example.parlamento.domain.repository.ProposicaoSpecs.doAno;
import static com.example.parlamento.domain.repository.ProposicaoSpecs.doNumero;
import static com.example.parlamento.domain.repository.ProposicaoSpecs.doTipo;
import static com.example.parlamento.domain.repository.ProposicaoSpecs.ementaContem;
import static com.example.parlamento.domain.repository.ProposicaoSpecs.naSituacao;

@Service
@Transactional(readOnly = true)
public class ProposicaoConsultaService {

	private final ProposicaoRepository repository;
	private final ProposicaoAutorRepository autorRepository;
	private final ProposicaoApiMapper mapper;

	public ProposicaoConsultaService(ProposicaoRepository repository, ProposicaoAutorRepository autorRepository,
			ProposicaoApiMapper mapper) {
		this.repository = repository;
		this.autorRepository = autorRepository;
		this.mapper = mapper;
	}

	public PaginaDto<ProposicaoResumoDto> listar(Integer ano, String tipo, Integer numero, String ementa,
			String situacao, Pageable pageable) {
		Specification<Proposicao> filtro = Specification.allOf(List.of(
				doAno(ano), doTipo(tipo), doNumero(numero), ementaContem(ementa), naSituacao(situacao)));
		return PaginaDto.de(repository.findAll(filtro, pageable), mapper::paraResumo, Proposicao::getAtualizadoEm);
	}

	/**
	 * Situações presentes na base, em ordem alfabética. Ordenadas aqui com regras do português:
	 * a ordenação do banco é byte a byte e poria acentuadas e minúsculas fora de lugar.
	 */
	public SituacoesDto listarSituacoes() {
		Collator portugues = Collator.getInstance(Locale.of("pt", "BR"));
		List<SituacoesDto.Item> itens = repository.contarPorSituacao().stream()
				.map(s -> new SituacoesDto.Item(s.getDescricao(), s.getTotal()))
				.sorted(Comparator.comparing(SituacoesDto.Item::descricao, portugues))
				.toList();
		return new SituacoesDto(itens);
	}

	public ProposicaoDto buscar(Long id) {
		return mapper.paraDto(buscarEntidade(id));
	}

	public AutoresDto listarAutores(Long id) {
		Proposicao proposicao = buscarEntidade(id);
		return mapper.paraAutores(proposicao, autorRepository.findByProposicaoIdOrderByOrdemAssinaturaAscIdAsc(id));
	}

	private Proposicao buscarEntidade(Long id) {
		return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Proposição", id));
	}
}
