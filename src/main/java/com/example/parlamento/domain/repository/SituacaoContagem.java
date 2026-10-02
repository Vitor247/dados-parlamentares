package com.example.parlamento.domain.repository;

/** Uma situação presente na base e quantas proposições (dentro de um filtro) estão nela. */
public record SituacaoContagem(String descricao, long total) {
}
