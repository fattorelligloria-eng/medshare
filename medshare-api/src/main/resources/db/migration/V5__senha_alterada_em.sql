-- Quando a senha foi trocada pela ultima vez.
--
-- Os tokens emitidos antes disso deixam de valer: quem troca a senha porque
-- desconfia que alguem entrou na conta precisa derrubar essa outra sessao, e
-- o token de renovacao dura 14 dias. Nulo para quem nunca trocou.
ALTER TABLE usuario ADD COLUMN senha_alterada_em TIMESTAMPTZ;
