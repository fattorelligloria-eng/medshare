-- =============================================================================
-- Catalogo completo a partir da lista de precos da CMED/ANVISA.
--
-- Na lista oficial, a unidade e a APRESENTACAO (ex.: "150 MG CAP CT 224"), nao
-- o registro: um mesmo registro tem varias apresentacoes, cada uma com seu
-- preco. Quem identifica a apresentacao e o codigo GGREM, entao ele vira a
-- chave da importacao, e o registro deixa de ser unico.
-- =============================================================================

ALTER TABLE medicamento DROP CONSTRAINT medicamento_registro_anvisa_key;

ALTER TABLE medicamento ADD COLUMN codigo_ggrem VARCHAR(15);
ALTER TABLE medicamento ADD CONSTRAINT medicamento_codigo_ggrem_unico UNIQUE (codigo_ggrem);
ALTER TABLE medicamento ADD CONSTRAINT medicamento_codigo_ggrem_numerico
    CHECK (codigo_ggrem IS NULL OR codigo_ggrem ~ '^[0-9]{15}$');

-- Associacoes de principios ativos passam de 1.400 caracteres na lista.
ALTER TABLE medicamento ALTER COLUMN principio_ativo TYPE TEXT;
ALTER TABLE medicamento ALTER COLUMN nome_comercial  TYPE VARCHAR(200);
ALTER TABLE medicamento ALTER COLUMN apresentacao    TYPE VARCHAR(300);
ALTER TABLE medicamento ALTER COLUMN laboratorio     TYPE VARCHAR(200);

-- A lista marca parte das apresentacoes com "- (*)": a tarja nao foi
-- informada. Registrar isso como "sem tarja" seria afirmar algo que a fonte
-- nao disse.
ALTER TABLE medicamento DROP CONSTRAINT medicamento_tarja_valida;
ALTER TABLE medicamento ADD CONSTRAINT medicamento_tarja_valida CHECK (
    tarja IN ('VERMELHA', 'PRETA', 'SEM_TARJA', 'NAO_INFORMADA')
);

-- A busca do app ignora acento e maiusculas ("acido" acha "ÁCIDO").
-- IMMUTABLE para poder ser usada em indice no futuro.
CREATE OR REPLACE FUNCTION sem_acento(texto TEXT) RETURNS TEXT AS $$
    SELECT lower(translate(texto,
        'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑáàâãäéèêëíìîïóòôõöúùûüçñ',
        'AAAAAEEEEIIIIOOOOOUUUUCNaaaaaeeeeiiiiooooouuuucn'));
$$ LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE;

CREATE INDEX idx_medicamento_ggrem ON medicamento (codigo_ggrem) WHERE codigo_ggrem IS NOT NULL;
