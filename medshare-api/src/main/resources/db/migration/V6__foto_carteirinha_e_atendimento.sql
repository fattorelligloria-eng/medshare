-- Foto de perfil, carteirinha digital e mensagens para a central.

-- -----------------------------------------------------------------------------
-- Foto de perfil.
--
-- Guarda so o endereco canonico da foto, o mesmo que o /api/fotos devolve. A
-- API nunca entrega este valor como esta: devolve um link assinado e com
-- prazo, igual ao das fotos de embalagem e de receita. Nulo enquanto a pessoa
-- nao colocar foto.
-- -----------------------------------------------------------------------------
ALTER TABLE usuario ADD COLUMN foto_url VARCHAR(300);

-- -----------------------------------------------------------------------------
-- Numero da carteirinha digital.
--
-- Doze digitos sorteados, nunca o id. O id e uma sequencia: com ele na
-- carteirinha, daria para adivinhar o numero de outras pessoas e saber
-- quantas usam o sistema.
--
-- Quem ja tem conta ganha o numero aqui; quem se cadastrar depois ganha no
-- cadastro (Usuario + ServicoDeAutenticacao).
-- -----------------------------------------------------------------------------
ALTER TABLE usuario ADD COLUMN numero_carteirinha VARCHAR(12);

DO $$
DECLARE
    pessoa    RECORD;
    candidato VARCHAR(12);
BEGIN
    FOR pessoa IN SELECT id FROM usuario WHERE numero_carteirinha IS NULL LOOP
        LOOP
            candidato := lpad(floor(random() * 1e12)::BIGINT::TEXT, 12, '0');
            EXIT WHEN NOT EXISTS (SELECT 1 FROM usuario WHERE numero_carteirinha = candidato);
        END LOOP;
        UPDATE usuario SET numero_carteirinha = candidato WHERE id = pessoa.id;
    END LOOP;
END $$;

ALTER TABLE usuario ALTER COLUMN numero_carteirinha SET NOT NULL;
ALTER TABLE usuario ADD CONSTRAINT usuario_carteirinha_unica UNIQUE (numero_carteirinha);
ALTER TABLE usuario ADD CONSTRAINT usuario_carteirinha_numerica CHECK (numero_carteirinha ~ '^[0-9]{12}$');

-- -----------------------------------------------------------------------------
-- Mensagens para a central (tela de Atendimento).
--
-- A pessoa escreve pelo aplicativo; a central responde pelo painel; a resposta
-- chega como notificacao e fica guardada junto da pergunta.
-- -----------------------------------------------------------------------------
CREATE TABLE mensagem_atendimento (
    id             BIGSERIAL     PRIMARY KEY,
    usuario_id     BIGINT        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    assunto        VARCHAR(30)   NOT NULL,
    texto          VARCHAR(2000) NOT NULL,
    criado_em      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    resposta       VARCHAR(2000),
    respondida_em  TIMESTAMPTZ,
    respondida_por BIGINT        REFERENCES usuario (id),

    CONSTRAINT mensagem_assunto_valido
        CHECK (assunto IN ('DUVIDA', 'PROBLEMA_COM_CAIXA', 'SUGESTAO_OU_RECLAMACAO', 'OUTRO')),
    -- Resposta, data e quem respondeu andam juntos: ou os tres, ou nenhum.
    CONSTRAINT mensagem_resposta_completa
        CHECK ((resposta IS NULL AND respondida_em IS NULL AND respondida_por IS NULL)
            OR (resposta IS NOT NULL AND respondida_em IS NOT NULL AND respondida_por IS NOT NULL))
);

CREATE INDEX idx_mensagem_usuario ON mensagem_atendimento (usuario_id, criado_em DESC);
-- A fila da central: so as que ainda esperam resposta, da mais antiga para a mais nova.
CREATE INDEX idx_mensagem_sem_resposta ON mensagem_atendimento (criado_em) WHERE resposta IS NULL;
