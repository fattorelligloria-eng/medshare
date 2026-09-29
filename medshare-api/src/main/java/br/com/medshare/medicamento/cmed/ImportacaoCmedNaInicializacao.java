package br.com.medshare.medicamento.cmed;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Importa a lista da CMED quando a API sobe, se MEDSHARE_CMED_ARQUIVO apontar
 * para o arquivo baixado. Roda antes dos dados de demonstracao e e seguro
 * repetir: cada execucao so atualiza precos e acrescenta o que for novo.
 *
 *     MEDSHARE_CMED_ARQUIVO=./dados/cmed/xls_conformidade_site_AAAAMMDD.xlsx
 */
@Component
@Order(0)
@ConditionalOnExpression("!'${medshare.cmed.arquivo:}'.isEmpty()")
public class ImportacaoCmedNaInicializacao implements ApplicationRunner {

    private final ImportadorCmed importador;
    private final Path arquivo;

    public ImportacaoCmedNaInicializacao(ImportadorCmed importador,
                                         @Value("${medshare.cmed.arquivo}") String arquivo) {
        this.importador = importador;
        this.arquivo = Path.of(arquivo);
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        importador.importar(arquivo);
    }
}
