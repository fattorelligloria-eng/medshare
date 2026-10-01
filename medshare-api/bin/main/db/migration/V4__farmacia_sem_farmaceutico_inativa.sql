-- Escopo "Por onde" / UC08: so aparece para doacao e retirada a farmacia com
-- farmaceutico responsavel (CRF). Farmacias cadastradas antes dessa regra e
-- sem ninguem vinculado ficam inativas ate o administrador vincular alguem —
-- sem farmaceutico, nenhuma caixa entregue la poderia ser conferida (RN01).
UPDATE ponto_coleta p
   SET ativo = FALSE
 WHERE ativo
   AND NOT EXISTS (SELECT 1 FROM farmaceutico f WHERE f.ponto_coleta_id = p.id);
