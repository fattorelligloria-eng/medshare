package br.com.medshare.reserva;

import br.com.medshare.comum.RegraDeNegocioViolada;

/**
 * UC07 A2 - a caixa chegou ao balcao sem a validade minima.
 *
 * Diferente das outras regras violadas, esta NAO desfaz a transacao: o
 * descarte da caixa e a volta do beneficiario a fila precisam ficar gravados
 * mesmo com a entrega recusada.
 */
public class ValidadeInsuficienteNaRetirada extends RegraDeNegocioViolada {

    public ValidadeInsuficienteNaRetirada(long diasRestantes, int diasMinimos) {
        super("RN02", ("A caixa vence em %d dia(s), abaixo dos %d dias mínimos para a retirada. "
                + "Ela foi descartada e o beneficiário voltou à fila com prioridade.")
                .formatted(diasRestantes, diasMinimos));
    }
}
