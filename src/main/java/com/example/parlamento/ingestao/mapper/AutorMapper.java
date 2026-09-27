package com.example.parlamento.ingestao.mapper;

import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.entity.Proposicao;
import com.example.parlamento.domain.entity.ProposicaoAutor;
import com.example.parlamento.ingestao.dto.AutorCamaraDto;

import java.util.Optional;

/**
 * Autores de proposição. O autor pode não ser deputado (Executivo, Senado, comissão...),
 * e a fonte não traz campo id: quando é deputado, o id está no final da URI.
 */
public final class AutorMapper {

	/** {@code codTipo} que a Câmara usa para "Deputado(a)". */
	static final int COD_TIPO_DEPUTADO = 10000;

	private AutorMapper() {
	}

	public static boolean ehDeputado(AutorCamaraDto dto) {
		if (dto.codTipo() != null) {
			return dto.codTipo() == COD_TIPO_DEPUTADO;
		}
		return dto.tipo() != null && dto.tipo().toLowerCase().startsWith("deputad");
	}

	/** Id do deputado, se o autor for deputado e a URI resolver. Nunca cria deputado a partir daqui. */
	public static Optional<Long> idDeputado(AutorCamaraDto dto) {
		return ehDeputado(dto) ? UriCamara.extrairId(dto.uri()) : Optional.empty();
	}

	/** @param deputado deputado já existente na base, ou null (não-deputado ou fora da base) */
	public static ProposicaoAutor paraEntidade(Proposicao proposicao, AutorCamaraDto dto, Deputado deputado) {
		ProposicaoAutor autor = new ProposicaoAutor(proposicao, dto.nome());
		autor.setDeputado(deputado);
		autor.setTipo(dto.tipo());
		autor.setCodTipo(dto.codTipo());
		autor.setOrdemAssinatura(dto.ordemAssinatura());
		autor.setProponente(dto.proponente());
		return autor;
	}
}
