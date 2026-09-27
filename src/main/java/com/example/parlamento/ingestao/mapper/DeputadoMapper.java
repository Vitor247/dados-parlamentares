package com.example.parlamento.ingestao.mapper;

import com.example.parlamento.domain.entity.Deputado;
import com.example.parlamento.domain.entity.Partido;
import com.example.parlamento.ingestao.dto.DeputadoDetalheCamaraDto;
import com.example.parlamento.ingestao.dto.DeputadoResumoCamaraDto;

import java.time.LocalDateTime;
import java.util.List;

/** DTOs externos de deputado → entidade. */
public final class DeputadoMapper {

	private DeputadoMapper() {
	}

	/**
	 * A lista {@code /deputados?idLegislatura=} repete o deputado uma vez por partido
	 * pelo qual passou na legislatura. Escolhe o registro do partido atual, usando a
	 * sigla de {@code ultimoStatus} do detalhe; sem detalhe (ou sem correspondência),
	 * fica com o último registro, que na fonte é o mais recente.
	 */
	public static DeputadoResumoCamaraDto registroAtual(
			List<DeputadoResumoCamaraDto> registros, DeputadoDetalheCamaraDto detalhe) {
		if (registros.isEmpty()) {
			throw new IllegalArgumentException("Lista de registros do deputado vazia");
		}
		String siglaAtual = detalhe == null || detalhe.ultimoStatus() == null
				? null
				: detalhe.ultimoStatus().siglaPartido();
		if (siglaAtual != null) {
			for (int i = registros.size() - 1; i >= 0; i--) {
				if (siglaAtual.equals(registros.get(i).siglaPartido())) {
					return registros.get(i);
				}
			}
		}
		return registros.getLast();
	}

	/** Campos vindos da lista, incluindo o vínculo com o partido (nunca do detalhe). */
	public static void atualizarDoResumo(Deputado deputado, DeputadoResumoCamaraDto dto,
			Partido partido, LocalDateTime agora) {
		deputado.setNome(dto.nome());
		deputado.setSiglaUf(dto.siglaUf());
		deputado.setIdLegislatura(dto.idLegislatura());
		deputado.setUrlFoto(dto.urlFoto());
		deputado.setEmail(dto.email());
		deputado.setSiglaPartido(dto.siglaPartido());
		deputado.setPartido(partido);
		deputado.setAtualizadoEm(agora);
	}

	/**
	 * Enriquecimento pelo detalhe: só dados pessoais e situação. Não toca em partido
	 * ({@code ultimoStatus.uriPartido} vem null) nem nos campos da lista.
	 */
	public static void atualizarDoDetalhe(Deputado deputado, DeputadoDetalheCamaraDto dto) {
		deputado.setNomeCivil(dto.nomeCivil());
		deputado.setDataNascimento(DatasCamara.data(dto.dataNascimento()));
		deputado.setUfNascimento(dto.ufNascimento());
		deputado.setMunicipioNascimento(dto.municipioNascimento());
		deputado.setEscolaridade(dto.escolaridade());
		if (dto.ultimoStatus() != null) {
			deputado.setSituacao(dto.ultimoStatus().situacao());
			deputado.setCondicaoEleitoral(dto.ultimoStatus().condicaoEleitoral());
		}
	}
}
