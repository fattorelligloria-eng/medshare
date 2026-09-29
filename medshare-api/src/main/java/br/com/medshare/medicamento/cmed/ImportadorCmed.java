package br.com.medshare.medicamento.cmed;

import br.com.medshare.comum.PropriedadesDoMedShare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importa a lista de precos de medicamentos publicada pela CMED/ANVISA.
 *
 * Fonte: gov.br/anvisa > Medicamentos > CMED > Precos, arquivo "PMC - xls".
 * A lista sai todo mes; rodar a importacao de novo atualiza precos e inclui
 * apresentacoes novas, sem duplicar nada — a chave e o codigo GGREM.
 *
 * O preco gravado e o PMC (preco maximo ao consumidor) na aliquota de ICMS de
 * Sao Paulo, onde a rede atua (RN09). E ele que decide o "alto custo" (RN07).
 * Apresentacoes sem PMC ficam de fora: sao de uso hospitalar e nao passam por
 * farmacia, entao nao ha como doa-las pela rede.
 */
@Service
public class ImportadorCmed {

    private static final Logger log = LoggerFactory.getLogger(ImportadorCmed.class);

    private static final int TAMANHO_DO_LOTE = 1000;
    private static final Pattern DATA_DE_PUBLICACAO =
            Pattern.compile("Publicada em (\\d{2}/\\d{2}/\\d{4})");

    private static final String GRAVAR = """
            INSERT INTO medicamento (codigo_ggrem, registro_anvisa, ean, nome_comercial,
                                     principio_ativo, apresentacao, laboratorio, pmc,
                                     vigencia_cmed, tarja, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            ON CONFLICT (codigo_ggrem) DO UPDATE SET
                registro_anvisa = EXCLUDED.registro_anvisa,
                ean             = EXCLUDED.ean,
                nome_comercial  = EXCLUDED.nome_comercial,
                principio_ativo = EXCLUDED.principio_ativo,
                apresentacao    = EXCLUDED.apresentacao,
                laboratorio     = EXCLUDED.laboratorio,
                pmc             = EXCLUDED.pmc,
                vigencia_cmed   = EXCLUDED.vigencia_cmed,
                tarja           = EXCLUDED.tarja,
                atualizado_em   = NOW()
            """;

    private final JdbcTemplate banco;
    private final PropriedadesDoMedShare propriedades;
    private final String colunaDoPmc;

    public ImportadorCmed(JdbcTemplate banco, PropriedadesDoMedShare propriedades,
                          @Value("${medshare.cmed.coluna-pmc:PMC 18 %}") String colunaDoPmc) {
        this.banco = banco;
        this.propriedades = propriedades;
        this.colunaDoPmc = colunaDoPmc;
    }

    public record Resultado(LocalDate publicacao, int linhasDeDados, int importadas,
                            int ignoradasSemPmc, int deAltoCusto) { }

    @Transactional
    public Resultado importar(Path arquivo) {
        if (!Files.isRegularFile(arquivo)) {
            throw new ArquivoCmedInvalido("Arquivo não encontrado: " + arquivo.toAbsolutePath());
        }

        var leitura = new Leitura(colunaDoPmc);
        LeitorDeXlsx.lerLinhas(arquivo, leitura::receber);
        if (leitura.colunas == null) {
            throw new ArquivoCmedInvalido(
                    "Não achei o cabeçalho da lista (coluna SUBSTÂNCIA). Este é o arquivo \"PMC - xls\" da CMED?");
        }

        LocalDate vigencia = leitura.publicacao.orElse(LocalDate.now());
        gravar(leitura.linhas, vigencia);

        BigDecimal piso = propriedades.doacao().valorMinimoPmc();
        int altoCusto = (int) leitura.linhas.stream().filter(l -> l.pmc().compareTo(piso) >= 0).count();
        var resultado = new Resultado(vigencia, leitura.linhasDeDados, leitura.linhas.size(),
                leitura.linhasDeDados - leitura.linhas.size(), altoCusto);
        log.info("Lista CMED de {} importada: {} apresentações gravadas ({} de alto custo), {} sem PMC ignoradas",
                vigencia, resultado.importadas(), altoCusto, resultado.ignoradasSemPmc());
        return resultado;
    }

    private void gravar(List<LinhaDaCmed> linhas, LocalDate vigencia) {
        Date dataDaVigencia = Date.valueOf(vigencia);
        for (int inicio = 0; inicio < linhas.size(); inicio += TAMANHO_DO_LOTE) {
            List<LinhaDaCmed> lote = linhas.subList(inicio, Math.min(inicio + TAMANHO_DO_LOTE, linhas.size()));
            banco.batchUpdate(GRAVAR, lote, lote.size(), (comando, linha) -> {
                comando.setString(1, linha.codigoGgrem());
                comando.setString(2, linha.registro());
                comando.setString(3, linha.ean());
                comando.setString(4, linha.nomeComercial());
                comando.setString(5, linha.principioAtivo());
                comando.setString(6, linha.apresentacao());
                comando.setString(7, linha.laboratorio());
                comando.setBigDecimal(8, linha.pmc());
                comando.setDate(9, dataDaVigencia);
                comando.setString(10, linha.tarja().name());
            });
        }
    }

    /** Estado da leitura: antes do cabecalho procura a data; depois, converte linhas. */
    private static final class Leitura {

        private final String colunaDoPmc;
        private LinhaDaCmed.Colunas colunas;
        private Optional<LocalDate> publicacao = Optional.empty();
        private final List<LinhaDaCmed> linhas = new ArrayList<>();
        private int linhasDeDados;

        Leitura(String colunaDoPmc) {
            this.colunaDoPmc = colunaDoPmc;
        }

        void receber(List<String> celulas) {
            if (colunas == null) {
                procurarDataDePublicacao(celulas);
                colunas = LinhaDaCmed.Colunas.doCabecalho(celulas, colunaDoPmc).orElse(null);
                return;
            }
            if (celulas.stream().allMatch(c -> c == null || c.isBlank())) {
                return;
            }
            linhasDeDados++;
            LinhaDaCmed.de(celulas, colunas).ifPresent(linhas::add);
        }

        private void procurarDataDePublicacao(List<String> celulas) {
            if (publicacao.isPresent() || celulas.isEmpty()) {
                return;
            }
            Matcher data = DATA_DE_PUBLICACAO.matcher(celulas.get(0));
            if (data.find()) {
                publicacao = Optional.of(LocalDate.parse(data.group(1),
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        }
    }
}
