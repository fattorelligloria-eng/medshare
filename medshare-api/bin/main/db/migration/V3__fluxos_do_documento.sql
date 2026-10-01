-- =============================================================================
-- Fluxos do documento de modelagem que faltavam (UC02 a UC10).
-- Cada bloco cita o caso de uso que motivou a mudanca.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- UC05/UC06 - oferta com prazo. A caixa disponivel e oferecida a uma pessoa da
-- fila por vez; ela tem 24 h para aceitar. Recusou ou nao respondeu, a oferta
-- passa para a proxima.
-- -----------------------------------------------------------------------------
CREATE TABLE oferta (
    id              BIGSERIAL   PRIMARY KEY,
    doacao_id       BIGINT      NOT NULL REFERENCES doacao (id),
    necessidade_id  BIGINT      NOT NULL REFERENCES necessidade (id),
    status          VARCHAR(12) NOT NULL DEFAULT 'PENDENTE',
    criada_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expira_em       TIMESTAMPTZ NOT NULL,
    respondida_em   TIMESTAMPTZ,

    CONSTRAINT oferta_status_valido CHECK (
        status IN ('PENDENTE', 'ACEITA', 'RECUSADA', 'EXPIRADA', 'CANCELADA')
    ),
    CONSTRAINT oferta_expiracao_futura CHECK (expira_em > criada_em)
);

-- Uma caixa so e oferecida a uma pessoa por vez, e uma pessoa so tem uma
-- oferta aberta por pedido: o banco segura a regra mesmo com dois jobs rodando.
CREATE UNIQUE INDEX uq_oferta_pendente_doacao      ON oferta (doacao_id)      WHERE status = 'PENDENTE';
CREATE UNIQUE INDEX uq_oferta_pendente_necessidade ON oferta (necessidade_id) WHERE status = 'PENDENTE';
CREATE INDEX idx_oferta_expiracao ON oferta (expira_em) WHERE status = 'PENDENTE';
CREATE INDEX idx_oferta_necessidade ON oferta (necessidade_id);

-- UC07 A2: quem perdeu a caixa por validade volta a fila na frente.
-- UC07 A1: receita que nao bateu deixa o pedido em revisao ate chegar outra.
ALTER TABLE necessidade ADD COLUMN prioridade     BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE necessidade ADD COLUMN em_revisao     BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE necessidade ADD COLUMN motivo_revisao VARCHAR(300);

-- -----------------------------------------------------------------------------
-- UC02 - horarios de funcionamento estruturados, para o app oferecer so os
-- horarios em que a farmacia esta aberta e tem vaga.
-- dias_de_funcionamento: 1 = segunda ... 7 = domingo (ISO-8601).
-- -----------------------------------------------------------------------------
ALTER TABLE ponto_coleta ADD COLUMN abre_as               TIME        NOT NULL DEFAULT '08:00';
ALTER TABLE ponto_coleta ADD COLUMN fecha_as              TIME        NOT NULL DEFAULT '18:00';
ALTER TABLE ponto_coleta ADD COLUMN dias_de_funcionamento VARCHAR(13) NOT NULL DEFAULT '1,2,3,4,5';
ALTER TABLE ponto_coleta ADD COLUMN vagas_por_hora        SMALLINT    NOT NULL DEFAULT 4;
ALTER TABLE ponto_coleta ADD CONSTRAINT ponto_horario_coerente CHECK (fecha_as > abre_as);
ALTER TABLE ponto_coleta ADD CONSTRAINT ponto_dias_validos
    CHECK (dias_de_funcionamento ~ '^[1-7](,[1-7])*$');
ALTER TABLE ponto_coleta ADD CONSTRAINT ponto_vagas_positivas CHECK (vagas_por_hora > 0);

-- Farmacias ja cadastradas com horario diferente do padrao (dados de
-- demonstracao): os campos novos passam a bater com o texto que ja exibiam.
UPDATE ponto_coleta SET dias_de_funcionamento = '1,2,3,4,5,6', fecha_as = '20:00'
    WHERE horario ILIKE 'Segunda a sábado, 8h às 20h';
UPDATE ponto_coleta SET abre_as = '09:00', fecha_as = '19:00'
    WHERE horario ILIKE 'Segunda a sexta, 9h às 19h';

-- UC02 A2 - o doador pode reagendar uma vez: a doacao passa a ter ate dois
-- agendamentos, e o lembrete da vespera e marcado para nao sair duas vezes.
ALTER TABLE agendamento DROP CONSTRAINT agendamento_doacao_id_key;
ALTER TABLE agendamento ADD COLUMN lembrete_enviado BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_agendamento_doacao ON agendamento (doacao_id, criado_em DESC);
CREATE UNIQUE INDEX uq_agendamento_codigo ON agendamento (codigo_entrega);

-- -----------------------------------------------------------------------------
-- UC01 - o doador declara que a embalagem esta lacrada (RN01).
-- -----------------------------------------------------------------------------
ALTER TABLE doacao ADD COLUMN lacre_declarado BOOLEAN NOT NULL DEFAULT FALSE;

-- UC10 A2 - a central pode pedir outra foto; cada foto nova gera uma analise
-- nova, e a antiga fica guardada.
ALTER TABLE analise_pre_validacao DROP CONSTRAINT analise_pre_validacao_doacao_id_key;
CREATE INDEX idx_analise_doacao ON analise_pre_validacao (doacao_id, criado_em DESC);

ALTER TABLE revisao_central DROP CONSTRAINT revisao_decisao_valida;
ALTER TABLE revisao_central ADD CONSTRAINT revisao_decisao_valida
    CHECK (decisao IN ('APROVADA', 'RECUSADA', 'NOVA_FOTO'));

-- UC03 - o farmaceutico fotografa a caixa na conferencia (exemplo para o
-- modelo da fase 2).
ALTER TABLE validacao ADD COLUMN foto_url VARCHAR(500);

-- -----------------------------------------------------------------------------
-- UC04 A2 - Portal da Transparencia fora do ar: o NIS fica guardado e uma
-- rotina tenta de novo depois.
-- -----------------------------------------------------------------------------
ALTER TABLE verificacao_cadunico ADD COLUMN consulta_pendente BOOLEAN NOT NULL DEFAULT FALSE;

-- -----------------------------------------------------------------------------
-- UC07 A3 - procuracao: so retira no lugar do beneficiario quem ele cadastrou
-- antes.
-- -----------------------------------------------------------------------------
CREATE TABLE procurador (
    id              BIGSERIAL    PRIMARY KEY,
    beneficiario_id BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    nome            VARCHAR(120) NOT NULL,
    cpf             VARCHAR(11)  NOT NULL,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT procurador_cpf_numerico CHECK (cpf ~ '^[0-9]{11}$'),
    CONSTRAINT procurador_unico UNIQUE (beneficiario_id, cpf)
);

ALTER TABLE entrega ADD COLUMN retirado_por_cpf        VARCHAR(11);
ALTER TABLE entrega ADD COLUMN retirado_por_procurador BOOLEAN NOT NULL DEFAULT FALSE;
