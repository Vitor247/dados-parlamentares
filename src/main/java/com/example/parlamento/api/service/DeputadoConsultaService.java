package com.example.parlamento.api.service;

import com.example.parlamento.api.dto.DeputadoDto;
import com.example.parlamento.api.dto.DeputadoResumoDto;
import com.example.parlamento.api.dto.PaginaDto;
import com.example.parlamento.api.dto.ProposicaoResumoDto;
import com.example.parlamento.api.mapper.DeputadoApiMapper;
import com.example.parlamento.api.mapper.ProposicaoApiMapper;
import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.entity.Proposicao;
import com.example.parlamento.domain.repository.DeputadoRepository;
import com.example.parlamento.domain.repository.ProposicaoAutorRepository;
import com.example.parlamento.domain.repository.ProposicaoRepository;
import com.example.parlamento.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.parlamento.domain.repository.ProposicaoSpecs.deAutoria;
import static com.example.parlamento.domain.repository.ProposicaoSpecs.doAno;
import static com.example.parlamento.domain.repository.ProposicaoSpecs.doTipo;

@Service
@Transactional(readOnly = true)
public class DeputadoConsultaService {

	private final DeputadoRepository repository;
	private final ProposicaoRepository proposicaoRepository;
	private final ProposicaoAutorRepository autorRepository;
	private final DeputadoApiMapper mapper;
	private final ProposicaoApiMapper proposicaoMapper;

	public DeputadoConsultaService(DeputadoRepository repository, ProposicaoRepository proposicaoRepository,
			ProposicaoAutorRepository autorRepository, DeputadoApiMapper mapper,
			ProposicaoApiMapper proposicaoMapper) {
		this.repository = repository;
		this.proposicaoRepository = proposicaoRepository;
		this.autorRepository = autorRepository;
		this.mapper = mapper;
		this.proposicaoMapper = proposicaoMapper;
	}

	public PaginaDto<DeputadoResumoDto> listar(Pageable pageable) {
		return PaginaDto.de(repository.findAll(pageable), mapper::paraResumo, Deputado::getAtualizadoEm);
	}

	public DeputadoDto buscar(Long id) {
		Deputado deputado = repository.findComPartidoById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Deputado", id));
		return mapper.paraDto(deputado, autorRepository.countByDeputadoId(id));
	}

	/** Proposições de autoria do deputado. Deputado inexistente é 404, não página vazia. */
	public PaginaDto<ProposicaoResumoDto> listarProposicoes(Long id, Integer ano, String tipo, Pageable pageable) {
		if (!repository.existsById(id)) {
			throw new RecursoNaoEncontradoException("Deputado", id);
		}
		Specification<Proposicao> filtro = Specification.allOf(List.of(deAutoria(id), doAno(ano), doTipo(tipo)));
		return PaginaDto.de(proposicaoRepository.findAll(filtro, pageable),
				proposicaoMapper::paraResumo, Proposicao::getAtualizadoEm);
	}
}
