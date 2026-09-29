package br.com.medshare.integracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Usada enquanto não há chave do Portal da Transparência.
 *
 * Confere apenas a forma do NIS, e diz isso na observação — que fica gravada no
 * banco. Assim ninguém olha um registro antigo e acha que houve consulta
 * oficial.
 */
@Component
@ConditionalOnExpression("'${medshare.portal-transparencia.chave:}'.isEmpty()")
public class ConsultaDeCadUnicoSimulada implements ConsultaDeCadUnico {

    private static final Logger log = LoggerFactory.getLogger(ConsultaDeCadUnicoSimulada.class);

    public ConsultaDeCadUnicoSimulada() {
        log.warn("Sem chave do Portal da Transparencia: a verificacao do CadUnico "
                + "confere apenas o formato do NIS, e todo caso vai para analise humana.");
    }

    @Override
    public ResultadoDaConsulta consultar(String nis) {
        if (!ValidadorDeNis.temFormatoValido(nis)) {
            return ResultadoDaConsulta.formatoInvalido(
                    "NIS inválido: o dígito verificador não confere");
        }
        return ResultadoDaConsulta.naoEncontrado(
                "NIS com formato válido - consulta oficial não realizada (modo simulado)");
    }

    @Override
    public String fonte() {
        return "SIMULADO";
    }
}
