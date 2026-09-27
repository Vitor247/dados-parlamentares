package com.example.parlamento.api.service;

import com.example.parlamento.api.dto.DeputadoResumoDto;
import com.example.parlamento.api.dto.PaginaDto;
import com.example.parlamento.api.dto.PartidoDto;
import com.example.parlamento.api.mapper.DeputadoApiMapper;
import com.example.parlamento.api.mapper.PartidoApiMapper;
import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.entity.Partido;
import com.example.parlamento.domain.repository.DeputadoRepository;
import com.example.parlamento.domain.repository.PartidoRepository;
import com.example.parlamento.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PartidoConsultaService {

	private final PartidoRepository repository;
	private final DeputadoRepository deputadoRepository;
	private final PartidoApiMapper mapper;
	private final DeputadoApiMapper deputadoMapper;

	public PartidoConsultaService(PartidoRepository repository, DeputadoRepository deputadoRepository,
			PartidoApiMapper mapper, DeputadoApiMapper deputadoMapper) {
		this.repository = repository;
		this.deputadoRepository = deputadoRepository;
		this.mapper = mapper;
		this.deputadoMapper = deputadoMapper;
	}

	public PaginaDto<PartidoDto> listar(Pageable pageable) {
		return PaginaDto.de(repository.findAll(pageable), mapper::paraDto, Partido::getAtualizadoEm);
	}

	public PartidoDto buscar(Long id) {
		return repository.findById(id)
				.map(mapper::paraDto)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Partido", id));
	}

	/** Deputados cujo partido atual (vínculo resolvido na ingestão) é este. */
	public PaginaDto<DeputadoResumoDto> listarDeputados(Long id, Pageable pageable) {
		if (!repository.existsById(id)) {
			throw new RecursoNaoEncontradoException("Partido", id);
		}
		return PaginaDto.de(deputadoRepository.findByPartidoId(id, pageable),
				deputadoMapper::paraResumo, Deputado::getAtualizadoEm);
	}
}
