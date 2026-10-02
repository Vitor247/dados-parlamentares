-- Filtro de proposições por situação. Filtra-se pelo texto, não pelo código: a Câmara usa
-- códigos diferentes com a mesma descrição (ex.: 905 e 1382), que o usuário não distingue.
CREATE INDEX idx_proposicao_situacao ON proposicao (situacao_descricao);
