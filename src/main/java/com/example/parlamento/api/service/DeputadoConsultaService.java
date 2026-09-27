package com.example.parlamento.api.service;

import com.example.parlamento.api.dto.DeputadoDto;
import com.example.parlamento.api.dto.DeputadoResumoDto;
import com.example.parlamento.api.dto.PaginaDto;
import com.example.parlamento.api.mapper.DeputadoApiMapper;
import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.repository.DeputadoRepository;
import com.example.parlamento.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DeputadoConsultaService {

	private final DeputadoRepository repository;
	private final DeputadoApiMapper mapper;

	public DeputadoConsultaService(DeputadoRepository repository, DeputadoApiMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	public PaginaDto<DeputadoResumoDto> listar(Pageable pageable) {
		return PaginaDto.de(repository.findAll(pageable), mapper::paraResumo, Deputado::getAtualizadoEm);
	}

	public DeputadoDto buscar(Long id) {
		return repository.findComPartidoById(id)
				.map(mapper::paraDto)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Deputado", id));
	}
}
