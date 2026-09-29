package br.com.medshare.medicamento.cmed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Leitor de XLSX: le a planilha em fluxo, sem carregar tudo")
class LeitorDeXlsxTest {

    @TempDir
    Path pasta;

    @Test
    @DisplayName("resolve textos compartilhados, texto inline, numero e celula pulada")
    void leCelulas() throws Exception {
        Path arquivo = planilha(
                """
                <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <si><t>SUBSTÂNCIA</t></si>
                  <si><r><t>ABA</t></r><r><t>TACEPTE</t></r></si>
                </sst>
                """,
                """
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
                  <row r="1"><c r="A1" t="s"><v>0</v></c></row>
                  <row r="2"><c r="A2" t="s"><v>1</v></c><c r="C2"><v>11336.31</v></c>
                             <c r="D2" t="inlineStr"><is><t>Tarja Preta</t></is></c></row>
                </sheetData></worksheet>
                """);

        List<List<String>> linhas = new ArrayList<>();
        LeitorDeXlsx.lerLinhas(arquivo, linhas::add);

        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0)).containsExactly("SUBSTÂNCIA");
        assertThat(linhas.get(1)).containsExactly("ABATACEPTE", "", "11336.31", "Tarja Preta");
    }

    @Test
    @DisplayName("converte a referencia da celula em indice de coluna")
    void indiceDaColuna() {
        assertThat(LeitorDeXlsx.indiceDaColuna("A7", 0)).isZero();
        assertThat(LeitorDeXlsx.indiceDaColuna("Z1", 0)).isEqualTo(25);
        assertThat(LeitorDeXlsx.indiceDaColuna("AA10", 0)).isEqualTo(26);
        assertThat(LeitorDeXlsx.indiceDaColuna("BV3", 0)).isEqualTo(73);   // ultima coluna da CMED
        assertThat(LeitorDeXlsx.indiceDaColuna(null, 4)).isEqualTo(4);
    }

    private Path planilha(String textos, String folha) throws Exception {
        Path arquivo = pasta.resolve("teste.xlsx");
        try (OutputStream saida = Files.newOutputStream(arquivo);
             ZipOutputStream zip = new ZipOutputStream(saida)) {
            escrever(zip, "xl/sharedStrings.xml", textos);
            escrever(zip, "xl/worksheets/sheet1.xml", folha);
        }
        return arquivo;
    }

    private void escrever(ZipOutputStream zip, String nome, String conteudo) throws Exception {
        zip.putNextEntry(new ZipEntry(nome));
        zip.write(conteudo.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
