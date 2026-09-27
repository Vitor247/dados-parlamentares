package com.example.parlamento.api.service;

import com.example.parlamento.api.dto.PaginaDto;
import com.example.parlamento.api.dto.PartidoDto;
import com.example.parlamento.api.mapper.PartidoApiMapper;
import com.example.parlamento.domain.entity.Partido;
import com.example.parlamento.domain.repository.PartidoRepository;
import com.example.parlamento.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PartidoConsultaService {

	private final PartidoRepository repository;
	private final PartidoApiMapper mapper;

	public PartidoConsultaService(PartidoRepository repository, PartidoApiMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	public PaginaDto<PartidoDto> listar(Pageable pageable) {
		return PaginaDto.de(repository.findAll(pageable), mapper::paraDto, Partido::getAtualizadoEm);
	}

	public PartidoDto buscar(Long id) {
		return repository.findById(id)
				.map(mapper::paraDto)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Partido", id));
	}
}
