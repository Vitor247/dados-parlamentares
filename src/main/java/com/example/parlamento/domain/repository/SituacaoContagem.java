package com.example.parlamento.domain.repository;

/** Projeção: uma situação presente na base e quantas proposições estão nela. */
public interface SituacaoContagem {

	String getDescricao();

	long getTotal();
}
