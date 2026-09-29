-- Momento em que situação, inteiro teor e autoria foram buscados na fonte (Fase B).
-- Separado de atualizado_em, que a Fase A renova todo dia ao regravar identificação e ementa:
-- sem isso, uma situação de meses atrás apareceria como "atualizada hoje".
ALTER TABLE proposicao ADD COLUMN detalhe_atualizado_em TIMESTAMP;

-- Proposições já enriquecidas: a melhor informação disponível é atualizado_em (no pior caso,
-- mais recente que o real, o que só adianta a revalidação).
UPDATE proposicao SET detalhe_atualizado_em = atualizado_em WHERE detalhe_carregado;

-- Seleção das situações mais antigas para revalidar.
CREATE INDEX idx_proposicao_detalhe_atualizado ON proposicao (detalhe_atualizado_em) WHERE detalhe_carregado;
