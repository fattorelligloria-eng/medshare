package br.com.medshare.doacao;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida da doacao, exatamente como no diagrama de estados do documento
 * de modelagem.
 *
 * Cada estado carrega a lista de estados para os quais ele pode ir. Isso mantem
 * o diagrama e o codigo na mesma fonte: para descobrir se uma transicao e
 * valida nao existe um "if" espalhado por tres services, existe uma pergunta
 * feita ao proprio estado.
 */
public enum StatusDoacao {

    /** Doador acabou de cadastrar a caixa pelo aplicativo. */
    CADASTRADA {
        @Override
        public Set<StatusDoacao> proximosPossiveis() {
            return EnumSet.of(PRE_VALIDADA, EM_ANALISE_CENTRAL, RECUSADA);
        }
    },

    /** RN10 - a IA achou divergencia ou nao teve certeza; um humano decide. */
    EM_ANALISE_CENTRAL {
        @Override
        public Set<StatusDoacao> proximosPossiveis() {
            return EnumSet.of(PRE_VALIDADA, RECUSADA);
        }
    },

    /** Leitura da embalagem bateu com o cadastro; pode escolher a farmacia. */
    PRE_VALIDADA {
        @Override
        public Set<StatusDoacao> proximosPossiveis() {
            return EnumSet.of(AGENDADA, RECUSADA);
        }
    },

    AGENDADA {
        @Override
        public Set<StatusDoacao> proximosPossiveis() {
            return EnumSet.of(RECEBIDA, CANCELADA);
        }
    },

    /** O farmaceutico recebeu a caixa no balcao. */
    RECEBIDA {
        @Override
        public Set<StatusDoacao> proximosPossiveis() {
            return EnumSet.of(VALIDADA, REJEITADA);
        }
    },

    /** RN01 - lacre conferido presencialmente e aprovado. */
    VALIDADA {
        @Override
        public Set<StatusDoacao> proximosPossiveis() {
            return EnumSet.of(DISPONIVEL);
        }
    },

    DISPONIVEL {
        @Override
        public Set<StatusDoacao> proximosPossiveis() {
            return EnumSet.of(RESERVADA, DESCARTADA);
        }
    },

    /** Volta para DISPONIVEL se o beneficiario nao retirar no prazo. */
    RESERVADA {
        @Override
        public Set<StatusDoacao> proximosPossiveis() {
            return EnumSet.of(ENTREGUE, DISPONIVEL, DESCARTADA);
        }
    },

    // --- estados finais: daqui a doacao nao sai mais ---

    /** Entregue ao beneficiario. Fim feliz do ciclo. */
    ENTREGUE,

    /** Barrada antes de chegar na farmacia (PMC abaixo do piso, validade curta). */
    RECUSADA,

    /** O doador nao compareceu ao agendamento. */
    CANCELADA,

    /** Chegou na farmacia, mas o farmaceutico reprovou na conferencia (RN01). */
    REJEITADA,

    /** Perdeu a validade minima enquanto esperava por um beneficiario. */
    DESCARTADA;

    /** Por padrao um estado e final. Os que nao sao sobrescrevem este metodo. */
    public Set<StatusDoacao> proximosPossiveis() {
        return Collections.emptySet();
    }

    public boolean podeIrPara(StatusDoacao destino) {
        return proximosPossiveis().contains(destino);
    }

    public boolean ehFinal() {
        return proximosPossiveis().isEmpty();
    }

    /** Estados em que a caixa ja esta fisicamente sob responsabilidade da rede. */
    public boolean estaSobCustodiaDaFarmacia() {
        return this == RECEBIDA || this == VALIDADA || this == DISPONIVEL || this == RESERVADA;
    }
}
