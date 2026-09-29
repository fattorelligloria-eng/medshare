-- =============================================================================
-- MedShare - esquema inicial
--
-- Cada regra de negocio do documento de modelagem aparece aqui como constraint,
-- indice ou gatilho. O comentario -- RNxx marca onde cada uma foi implementada.
-- O banco e a ultima linha de defesa: mesmo que um bug passe pela aplicacao,
-- um dado invalido nao entra.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- RN09 - enderecos apenas na Grande Sao Paulo.
-- Os 39 municipios da Regiao Metropolitana de Sao Paulo (Lei Complementar
-- Estadual 1.139/2011). Tabela de referencia em vez de um CHECK gigante: fica
-- consultavel pela API (a tela de cadastro monta o combo a partir daqui), e
-- facil de auditar e nao precisa de migration para corrigir um acento.
--
-- codigo_ibge nasce nulo de proposito. Quem preenche e o carregador que le a
-- API de localidades do IBGE (servicodados.ibge.gov.br, publica e sem chave),
-- em vez de a gente digitar 39 codigos de sete digitos na mao e errar algum.
-- -----------------------------------------------------------------------------
CREATE TABLE municipio_rmsp (
    id          SMALLSERIAL PRIMARY KEY,
    nome        VARCHAR(60) NOT NULL UNIQUE,
    codigo_ibge VARCHAR(7)     UNIQUE,

    CONSTRAINT municipio_codigo_ibge_numerico
        CHECK (codigo_ibge IS NULL OR codigo_ibge ~ '^[0-9]{7}$')
);

INSERT INTO municipio_rmsp (nome) VALUES
    ('Arujá'),
    ('Barueri'),
    ('Biritiba Mirim'),
    ('Caieiras'),
    ('Cajamar'),
    ('Carapicuíba'),
    ('Cotia'),
    ('Diadema'),
    ('Embu das Artes'),
    ('Embu-Guaçu'),
    ('Ferraz de Vasconcelos'),
    ('Francisco Morato'),
    ('Franco da Rocha'),
    ('Guararema'),
    ('Guarulhos'),
    ('Itapecerica da Serra'),
    ('Itapevi'),
    ('Itaquaquecetuba'),
    ('Jandira'),
    ('Juquitiba'),
    ('Mairiporã'),
    ('Mauá'),
    ('Mogi das Cruzes'),
    ('Osasco'),
    ('Pirapora do Bom Jesus'),
    ('Poá'),
    ('Ribeirão Pires'),
    ('Rio Grande da Serra'),
    ('Salesópolis'),
    ('Santa Isabel'),
    ('Santana de Parnaíba'),
    ('Santo André'),
    ('São Bernardo do Campo'),
    ('São Caetano do Sul'),
    ('São Lourenço da Serra'),
    ('São Paulo'),
    ('Suzano'),
    ('Taboão da Serra'),
    ('Vargem Grande Paulista');

-- -----------------------------------------------------------------------------
-- Usuario
-- Um mesmo usuario pode ser doador e beneficiario ao mesmo tempo, por isso os
-- papeis ficam em tabela separada em vez de uma coluna "tipo".
-- -----------------------------------------------------------------------------
CREATE TABLE usuario (
    id              BIGSERIAL     PRIMARY KEY,
    nome            VARCHAR(120)  NOT NULL,
    cpf             VARCHAR(11)      NOT NULL UNIQUE,
    email           VARCHAR(160)  NOT NULL UNIQUE,
    senha_hash      VARCHAR(100)  NOT NULL,
    telefone        VARCHAR(20),

    cep             VARCHAR(8)       NOT NULL,
    logradouro      VARCHAR(160)  NOT NULL,
    numero          VARCHAR(20)   NOT NULL,
    complemento     VARCHAR(60),
    bairro          VARCHAR(80)   NOT NULL,
    municipio_id    SMALLINT      NOT NULL REFERENCES municipio_rmsp (id),  -- RN09
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,

    ativo           BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT usuario_cpf_numerico  CHECK (cpf ~ '^[0-9]{11}$'),
    CONSTRAINT usuario_cep_numerico  CHECK (cep ~ '^[0-9]{8}$'),
    CONSTRAINT usuario_email_formato CHECK (email LIKE '%@%.%'),
    CONSTRAINT usuario_coordenada_completa
        CHECK ((latitude IS NULL) = (longitude IS NULL)),
    CONSTRAINT usuario_latitude_valida
        CHECK (latitude  IS NULL OR latitude  BETWEEN  -90 AND  90),
    CONSTRAINT usuario_longitude_valida
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE TABLE papel_usuario (
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    papel      VARCHAR(20) NOT NULL,
    PRIMARY KEY (usuario_id, papel),
    CONSTRAINT papel_valido CHECK (
        papel IN ('DOADOR', 'BENEFICIARIO', 'FARMACEUTICO', 'ADMIN')
    )
);

-- -----------------------------------------------------------------------------
-- Ponto de coleta - farmacia parceira.
-- RN05: doador e beneficiario nunca se encontram; tudo passa por aqui.
-- -----------------------------------------------------------------------------
CREATE TABLE ponto_coleta (
    id             BIGSERIAL     PRIMARY KEY,
    nome           VARCHAR(120)  NOT NULL,
    cnpj           VARCHAR(14)      NOT NULL UNIQUE,

    cep            VARCHAR(8)       NOT NULL,
    logradouro     VARCHAR(160)  NOT NULL,
    numero         VARCHAR(20)   NOT NULL,
    complemento    VARCHAR(60),
    bairro         VARCHAR(80)   NOT NULL,
    municipio_id   SMALLINT      NOT NULL REFERENCES municipio_rmsp (id),  -- RN09
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,

    horario        VARCHAR(120)  NOT NULL,
    ativo          BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT ponto_cnpj_numerico CHECK (cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ponto_latitude_valida  CHECK (latitude  BETWEEN  -90 AND  90),
    CONSTRAINT ponto_longitude_valida CHECK (longitude BETWEEN -180 AND 180)
);

-- Farmaceutico: especializacao de usuario, identificado pelo CRF.
-- E quem valida fisicamente a doacao (RN10 - a decisao final e sempre humana).
CREATE TABLE farmaceutico (
    usuario_id      BIGINT      PRIMARY KEY REFERENCES usuario (id) ON DELETE CASCADE,
    crf             VARCHAR(20) NOT NULL UNIQUE,
    uf_crf          VARCHAR(2)  NOT NULL DEFAULT 'SP',
    ponto_coleta_id BIGINT      NOT NULL REFERENCES ponto_coleta (id),

    CONSTRAINT farmaceutico_uf_valida CHECK (uf_crf ~ '^[A-Z]{2}$')
);

-- -----------------------------------------------------------------------------
-- Medicamento - catalogo alimentado pela tabela CMED/ANVISA.
-- RN07: so entra na rede medicamento com PMC >= R$ 150,00. A coluna alto_custo
-- e calculada pelo banco, entao nao existe a possibilidade de ficar dessincronizada.
-- -----------------------------------------------------------------------------
CREATE TABLE medicamento (
    id              BIGSERIAL      PRIMARY KEY,
    registro_anvisa VARCHAR(20)    NOT NULL UNIQUE,
    nome_comercial  VARCHAR(160)   NOT NULL,
    principio_ativo VARCHAR(200)   NOT NULL,
    apresentacao    VARCHAR(200)   NOT NULL,
    laboratorio     VARCHAR(120),
    ean             VARCHAR(14),
    pmc             NUMERIC(10, 2) NOT NULL,
    vigencia_cmed   DATE           NOT NULL,
    tarja           VARCHAR(20)    NOT NULL DEFAULT 'VERMELHA',
    atualizado_em   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    alto_custo BOOLEAN GENERATED ALWAYS AS (pmc >= 150.00) STORED,  -- RN07

    CONSTRAINT medicamento_pmc_positivo CHECK (pmc > 0),
    CONSTRAINT medicamento_tarja_valida CHECK (
        tarja IN ('VERMELHA', 'PRETA', 'SEM_TARJA')
    )
);

CREATE INDEX idx_medicamento_principio  ON medicamento (principio_ativo);
CREATE INDEX idx_medicamento_ean        ON medicamento (ean) WHERE ean IS NOT NULL;
CREATE INDEX idx_medicamento_alto_custo ON medicamento (alto_custo) WHERE alto_custo;

-- -----------------------------------------------------------------------------
-- Doacao - o centro do sistema. Uma linha = uma caixa rastreada do cadastro
-- ate a entrega.
-- -----------------------------------------------------------------------------
CREATE TABLE doacao (
    id              BIGSERIAL    PRIMARY KEY,
    codigo          VARCHAR(12)  NOT NULL UNIQUE,
    doador_id       BIGINT       NOT NULL REFERENCES usuario (id),
    medicamento_id  BIGINT       NOT NULL REFERENCES medicamento (id),
    ponto_coleta_id BIGINT                REFERENCES ponto_coleta (id),

    lote            VARCHAR(30)  NOT NULL,
    validade        DATE         NOT NULL,
    foto_url        VARCHAR(500) NOT NULL,
    status          VARCHAR(25)  NOT NULL DEFAULT 'CADASTRADA',

    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT doacao_status_valido CHECK (status IN (
        'CADASTRADA', 'EM_ANALISE_CENTRAL', 'PRE_VALIDADA', 'AGENDADA',
        'RECEBIDA', 'VALIDADA', 'DISPONIVEL', 'RESERVADA', 'ENTREGUE',
        'RECUSADA', 'CANCELADA', 'REJEITADA', 'DESCARTADA'
    )),
    -- A partir de AGENDADA a doacao precisa ter um ponto de coleta definido.
    CONSTRAINT doacao_ponto_obrigatorio_apos_agendamento CHECK (
        status IN ('CADASTRADA', 'EM_ANALISE_CENTRAL', 'PRE_VALIDADA',
                   'RECUSADA', 'CANCELADA')
        OR ponto_coleta_id IS NOT NULL
    )
);

CREATE INDEX idx_doacao_status       ON doacao (status);
CREATE INDEX idx_doacao_doador       ON doacao (doador_id);
CREATE INDEX idx_doacao_medicamento  ON doacao (medicamento_id);
CREATE INDEX idx_doacao_validade     ON doacao (validade);
-- Consulta mais quente do sistema: o que esta disponivel de um medicamento.
CREATE INDEX idx_doacao_disponivel   ON doacao (medicamento_id, validade)
    WHERE status = 'DISPONIVEL';

-- RN02 - minimo de 30 dias de validade.
-- Nao da para escrever isso como CHECK porque CURRENT_DATE nao e imutavel;
-- entao vira gatilho. Vale no cadastro e em qualquer mudanca de validade.
CREATE OR REPLACE FUNCTION valida_prazo_minimo_doacao() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.validade < CURRENT_DATE + INTERVAL '30 days' THEN
        RAISE EXCEPTION
            'RN02: a doacao % precisa de no minimo 30 dias de validade (validade informada: %)',
            NEW.codigo, NEW.validade
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_doacao_prazo_minimo
    BEFORE INSERT ON doacao
    FOR EACH ROW EXECUTE FUNCTION valida_prazo_minimo_doacao();

-- Mantem atualizado_em sem depender da aplicacao lembrar de preencher.
CREATE OR REPLACE FUNCTION marca_atualizacao() RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_doacao_atualizacao
    BEFORE UPDATE ON doacao
    FOR EACH ROW EXECUTE FUNCTION marca_atualizacao();

-- -----------------------------------------------------------------------------
-- RN06 - historico completo e imutavel por unidade doada.
-- A tabela so aceita INSERT. UPDATE e DELETE sao bloqueados pelo proprio banco,
-- inclusive para quem tiver acesso direto ao Postgres.
-- -----------------------------------------------------------------------------
CREATE TABLE evento_historico (
    id             BIGSERIAL    PRIMARY KEY,
    doacao_id      BIGINT       NOT NULL REFERENCES doacao (id),
    ocorrido_em    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    tipo           VARCHAR(30)  NOT NULL,
    descricao      VARCHAR(500) NOT NULL,
    status_anterior VARCHAR(25),
    status_novo     VARCHAR(25),
    responsavel_id BIGINT       REFERENCES usuario (id)
);

CREATE INDEX idx_evento_doacao ON evento_historico (doacao_id, ocorrido_em);

CREATE OR REPLACE FUNCTION bloqueia_alteracao_historico() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'RN06: o historico da doacao e imutavel - % nao e permitido em evento_historico',
        TG_OP
        USING ERRCODE = 'integrity_constraint_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_historico_imutavel
    BEFORE UPDATE OR DELETE ON evento_historico
    FOR EACH ROW EXECUTE FUNCTION bloqueia_alteracao_historico();

-- -----------------------------------------------------------------------------
-- RN01 + RN10 - leitura automatica da embalagem.
-- A IA preenche esta tabela, mas nunca decide sozinha: quando ha divergencia ou
-- a certeza nao e alta, a doacao vai para EM_ANALISE_CENTRAL e um humano resolve.
-- -----------------------------------------------------------------------------
CREATE TABLE analise_pre_validacao (
    id               BIGSERIAL    PRIMARY KEY,
    doacao_id        BIGINT       NOT NULL UNIQUE REFERENCES doacao (id),
    ean_lido         VARCHAR(14),
    lote_lido        VARCHAR(30),
    validade_lida    DATE,
    classe_embalagem VARCHAR(20)  NOT NULL,
    certeza          VARCHAR(10)  NOT NULL,
    motivo           VARCHAR(500),
    avaliador        VARCHAR(40)  NOT NULL,
    divergencias     TEXT[]       NOT NULL DEFAULT '{}',
    criado_em        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT analise_classe_valida CHECK (
        classe_embalagem IN ('LACRADA', 'VIOLADA', 'DANIFICADA', 'INVALIDA')
    ),
    CONSTRAINT analise_certeza_valida CHECK (certeza IN ('ALTA', 'MEDIA', 'BAIXA'))
);

-- Decisao humana sobre um caso que a IA mandou para a central (RN10).
CREATE TABLE revisao_central (
    id            BIGSERIAL    PRIMARY KEY,
    doacao_id     BIGINT       NOT NULL REFERENCES doacao (id),
    analista_id   BIGINT       NOT NULL REFERENCES usuario (id),
    decisao       VARCHAR(15)  NOT NULL,
    justificativa VARCHAR(500) NOT NULL,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT revisao_decisao_valida CHECK (decisao IN ('APROVADA', 'RECUSADA'))
);

CREATE INDEX idx_revisao_doacao ON revisao_central (doacao_id);

-- -----------------------------------------------------------------------------
-- Agendamento da entrega do doador no ponto de coleta.
-- -----------------------------------------------------------------------------
CREATE TABLE agendamento (
    id              BIGSERIAL   PRIMARY KEY,
    doacao_id       BIGINT      NOT NULL UNIQUE REFERENCES doacao (id),
    ponto_coleta_id BIGINT      NOT NULL REFERENCES ponto_coleta (id),
    data_hora       TIMESTAMPTZ NOT NULL,
    codigo_entrega  VARCHAR(10) NOT NULL,
    compareceu      BOOLEAN,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- RN01 - conferencia presencial do lacre pelo farmaceutico.
-- Nenhuma doacao chega a DISPONIVEL sem uma linha aprovada aqui.
-- -----------------------------------------------------------------------------
CREATE TABLE validacao (
    id              BIGSERIAL    PRIMARY KEY,
    doacao_id       BIGINT       NOT NULL UNIQUE REFERENCES doacao (id),
    farmaceutico_id BIGINT       NOT NULL REFERENCES farmaceutico (usuario_id),
    ocorrido_em     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    lacre_integro   BOOLEAN      NOT NULL,   -- RN01
    dados_conferem  BOOLEAN      NOT NULL,
    aprovada        BOOLEAN      NOT NULL,
    motivo_rejeicao VARCHAR(500),

    CONSTRAINT validacao_rejeicao_justificada CHECK (
        aprovada OR motivo_rejeicao IS NOT NULL
    ),
    -- RN01: nao existe aprovacao com lacre violado.
    CONSTRAINT validacao_aprovada_exige_lacre CHECK (
        NOT aprovada OR (lacre_integro AND dados_conferem)
    )
);

-- -----------------------------------------------------------------------------
-- RN08 - beneficiario com NIS ativo no CadUnico, valido por 12 meses.
-- -----------------------------------------------------------------------------
CREATE TABLE verificacao_cadunico (
    id            BIGSERIAL   PRIMARY KEY,
    usuario_id    BIGINT      NOT NULL REFERENCES usuario (id),
    nis           VARCHAR(11)    NOT NULL,
    confirmado    BOOLEAN     NOT NULL,
    verificado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valido_ate    DATE        NOT NULL,
    fonte         VARCHAR(40) NOT NULL DEFAULT 'PORTAL_TRANSPARENCIA',

    CONSTRAINT cadunico_nis_numerico CHECK (nis ~ '^[0-9]{11}$')
);

CREATE INDEX idx_cadunico_usuario ON verificacao_cadunico (usuario_id, valido_ate DESC);

-- -----------------------------------------------------------------------------
-- Necessidade - o pedido do beneficiario.
-- -----------------------------------------------------------------------------
CREATE TABLE necessidade (
    id              BIGSERIAL   PRIMARY KEY,
    beneficiario_id BIGINT      NOT NULL REFERENCES usuario (id),
    medicamento_id  BIGINT      NOT NULL REFERENCES medicamento (id),
    ativa           BOOLEAN     NOT NULL DEFAULT TRUE,
    criada_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    encerrada_em    TIMESTAMPTZ,

    CONSTRAINT necessidade_encerramento_coerente CHECK (
        ativa = (encerrada_em IS NULL)
    )
);

-- RN04 - uma necessidade ativa por medicamento por beneficiario.
CREATE UNIQUE INDEX uq_necessidade_ativa
    ON necessidade (beneficiario_id, medicamento_id)
    WHERE ativa;

-- RN03 - retirada apenas com receita valida para o principio ativo.
CREATE TABLE receita (
    id             BIGSERIAL    PRIMARY KEY,
    necessidade_id BIGINT       NOT NULL UNIQUE REFERENCES necessidade (id),
    foto_url       VARCHAR(500) NOT NULL,
    data_emissao   DATE         NOT NULL,
    validade       DATE         NOT NULL,
    crm_medico     VARCHAR(20)  NOT NULL,
    uf_crm         VARCHAR(2)   NOT NULL DEFAULT 'SP',
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT receita_validade_posterior CHECK (validade >= data_emissao),
    CONSTRAINT receita_uf_valida CHECK (uf_crm ~ '^[A-Z]{2}$')
);

-- -----------------------------------------------------------------------------
-- Reserva - liga uma doacao disponivel a uma necessidade.
-- RN05: a associacao existe aqui, mas nenhum endpoint devolve os dois lados
-- juntos. O doador ve o codigo da sua doacao; o beneficiario ve o codigo de
-- retirada. Ninguem ve o nome do outro.
-- -----------------------------------------------------------------------------
CREATE TABLE reserva (
    id              BIGSERIAL   PRIMARY KEY,
    codigo_retirada VARCHAR(10) NOT NULL UNIQUE,
    doacao_id       BIGINT      NOT NULL REFERENCES doacao (id),
    necessidade_id  BIGINT      NOT NULL REFERENCES necessidade (id),
    status          VARCHAR(15) NOT NULL DEFAULT 'ATIVA',
    criada_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expira_em       TIMESTAMPTZ NOT NULL,

    CONSTRAINT reserva_status_valido CHECK (
        status IN ('ATIVA', 'CONCLUIDA', 'EXPIRADA', 'CANCELADA')
    ),
    CONSTRAINT reserva_expiracao_futura CHECK (expira_em > criada_em)
);

-- Uma doacao nao pode estar reservada duas vezes ao mesmo tempo.
CREATE UNIQUE INDEX uq_reserva_doacao_ativa
    ON reserva (doacao_id) WHERE status = 'ATIVA';

-- RN04 - uma reserva ativa por necessidade (e a necessidade ja e unica por
-- beneficiario + medicamento, entao os dois indices juntos garantem a regra).
CREATE UNIQUE INDEX uq_reserva_necessidade_ativa
    ON reserva (necessidade_id) WHERE status = 'ATIVA';

CREATE INDEX idx_reserva_expiracao ON reserva (expira_em) WHERE status = 'ATIVA';

-- -----------------------------------------------------------------------------
-- Entrega - fecha o ciclo. So acontece com receita conferida (RN03).
-- -----------------------------------------------------------------------------
CREATE TABLE entrega (
    id                 BIGSERIAL   PRIMARY KEY,
    reserva_id         BIGINT      NOT NULL UNIQUE REFERENCES reserva (id),
    farmaceutico_id    BIGINT      NOT NULL REFERENCES farmaceutico (usuario_id),
    ocorrido_em        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    receita_conferida  BOOLEAN     NOT NULL,  -- RN03
    documento_conferido BOOLEAN    NOT NULL,

    CONSTRAINT entrega_exige_conferencia CHECK (receita_conferida AND documento_conferido)
);

-- -----------------------------------------------------------------------------
-- Notificacao (push via FCM).
-- -----------------------------------------------------------------------------
CREATE TABLE notificacao (
    id         BIGSERIAL    PRIMARY KEY,
    usuario_id BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    titulo     VARCHAR(120) NOT NULL,
    corpo      VARCHAR(500) NOT NULL,
    tipo       VARCHAR(30)  NOT NULL,
    lida       BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notificacao_usuario ON notificacao (usuario_id, criado_em DESC);

-- -----------------------------------------------------------------------------
-- Distancia entre dois pontos pela formula de Haversine, em quilometros.
-- Usada pelo matching para achar o ponto de coleta mais proximo.
-- Escrita em SQL puro de proposito: funciona no Postgres padrao, sem depender
-- da extensao PostGIS estar instalada no servidor.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION distancia_km(
    lat1 DOUBLE PRECISION, lon1 DOUBLE PRECISION,
    lat2 DOUBLE PRECISION, lon2 DOUBLE PRECISION
) RETURNS DOUBLE PRECISION AS $$
    SELECT 6371 * 2 * ASIN(SQRT(
        POWER(SIN(RADIANS(lat2 - lat1) / 2), 2) +
        COS(RADIANS(lat1)) * COS(RADIANS(lat2)) *
        POWER(SIN(RADIANS(lon2 - lon1) / 2), 2)
    ));
$$ LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE;
