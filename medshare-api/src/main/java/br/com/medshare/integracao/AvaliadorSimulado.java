package br.com.medshare.integracao;

import br.com.medshare.prevalidacao.Certeza;
import br.com.medshare.prevalidacao.ClasseEmbalagem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Avaliador usado quando nao ha chave do Gemini configurada.
 *
 * Existe para que qualquer pessoa consiga clonar o projeto e rodar o fluxo
 * inteiro sem chave de API nenhuma — util na apresentacao e nos testes.
 *
 * Ele devolve sempre certeza BAIXA de proposito. O resultado pratico e que toda
 * doacao cai na fila da central: e o comportamento seguro, e deixa claro que
 * nada foi aprovado automaticamente por um simulador.
 */
@Component
@ConditionalOnExpression("'${medshare.gemini.chave:}'.isEmpty()")
public class AvaliadorSimulado implements AvaliadorDeEmbalagem {

    private static final Logger log = LoggerFactory.getLogger(AvaliadorSimulado.class);

    public AvaliadorSimulado() {
        log.warn("Sem chave do Gemini configurada: usando avaliador simulado. "
                + "Toda doacao ira para analise da central.");
    }

    @Override
    public LeituraDaEmbalagem avaliar(String fotoUrl) {
        return new LeituraDaEmbalagem(null, null, null,
                ClasseEmbalagem.INVALIDA, Certeza.BAIXA,
                "Avaliador simulado: configure MEDSHARE_GEMINI_CHAVE para a leitura real");
    }

    @Override
    public String nome() {
        return "simulado";
    }
}
