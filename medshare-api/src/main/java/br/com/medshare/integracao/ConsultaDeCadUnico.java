package br.com.medshare.integracao;

/**
 * RN08 - confirma se quem pede o medicamento está em programa social.
 *
 * Não existe API pública que consulte o CadÚnico diretamente — isso não é
 * oferecido a ninguém, nem mediante pagamento. O que existe, gratuito e
 * oficial, é o Portal da Transparência, que informa se um NIS aparece como
 * beneficiário do Bolsa Família.
 *
 * A diferença importa e está assumida no projeto: quem está no CadÚnico mas
 * não recebe Bolsa Família não aparece nessa consulta. Por isso "não
 * encontrado" nunca é recusa — é encaminhamento para conferência humana, o
 * mesmo caminho que a foto duvidosa segue na RN10.
 */
public interface ConsultaDeCadUnico {

    ResultadoDaConsulta consultar(String nis);

    String fonte();

    /**
     * @param confirmado        apareceu como beneficiário e pode reservar já
     * @param formatoValido     o número tem a forma de um NIS de verdade
     * @param observacao        o que dizer a quem está esperando
     */
    record ResultadoDaConsulta(boolean confirmado, boolean formatoValido, String observacao,
                               boolean indisponivel) {

        public ResultadoDaConsulta(boolean confirmado, boolean formatoValido, String observacao) {
            this(confirmado, formatoValido, observacao, false);
        }

        public static ResultadoDaConsulta confirmado(String observacao) {
            return new ResultadoDaConsulta(true, true, observacao);
        }

        public static ResultadoDaConsulta naoEncontrado(String observacao) {
            return new ResultadoDaConsulta(false, true, observacao);
        }

        /** UC04 A2 - o Portal nao respondeu: guarda o NIS e tenta de novo depois. */
        public static ResultadoDaConsulta indisponivel(String observacao) {
            return new ResultadoDaConsulta(false, true, observacao, true);
        }

        public static ResultadoDaConsulta formatoInvalido(String observacao) {
            return new ResultadoDaConsulta(false, false, observacao);
        }

        /** Formato bom mas sem confirmação: uma pessoa precisa olhar. */
        public boolean precisaDeAnaliseHumana() {
            return !confirmado && formatoValido && !indisponivel;
        }
    }
}
