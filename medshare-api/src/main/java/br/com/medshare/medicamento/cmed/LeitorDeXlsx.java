package br.com.medshare.medicamento.cmed;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Le a primeira planilha de um arquivo .xlsx, linha por linha.
 *
 * A lista da CMED tem 26 mil linhas e 74 colunas: o XML da planilha passa de
 * 60 MB. Carregar isso inteiro em memoria (como faz o Apache POI no modo
 * comum) e desperdicio; aqui o XML e lido em fluxo (StAX) e cada linha e
 * entregue assim que termina. So a tabela de textos compartilhados fica em
 * memoria, porque as celulas apontam para ela por indice.
 *
 * Suporta o que a planilha da ANVISA usa: texto compartilhado, texto inline e
 * numero. Formula e data nao sao interpretadas.
 */
public final class LeitorDeXlsx {

    private static final String PLANILHA = "xl/worksheets/sheet1.xml";
    private static final String TEXTOS = "xl/sharedStrings.xml";

    private LeitorDeXlsx() { }

    /** Chama {@code aoLerLinha} para cada linha, com as celulas nas posicoes das colunas. */
    public static void lerLinhas(Path arquivo, Consumer<List<String>> aoLerLinha) {
        try (ZipFile zip = new ZipFile(arquivo.toFile())) {
            List<String> textos = lerTextosCompartilhados(zip);
            ZipEntry planilha = zip.getEntry(PLANILHA);
            if (planilha == null) {
                throw new ArquivoCmedInvalido("O arquivo não tem a planilha " + PLANILHA);
            }
            try (InputStream entrada = zip.getInputStream(planilha)) {
                lerPlanilha(entrada, textos, aoLerLinha);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não consegui abrir " + arquivo, e);
        } catch (XMLStreamException e) {
            throw new ArquivoCmedInvalido("A planilha está corrompida: " + e.getMessage());
        }
    }

    private static List<String> lerTextosCompartilhados(ZipFile zip)
            throws IOException, XMLStreamException {
        List<String> textos = new ArrayList<>();
        ZipEntry entrada = zip.getEntry(TEXTOS);
        if (entrada == null) {
            return textos;
        }
        try (InputStream fluxo = zip.getInputStream(entrada)) {
            XMLStreamReader xml = fabrica().createXMLStreamReader(fluxo);
            StringBuilder atual = null;
            while (xml.hasNext()) {
                int evento = xml.next();
                if (evento == XMLStreamConstants.START_ELEMENT) {
                    if ("si".equals(xml.getLocalName())) {
                        atual = new StringBuilder();
                    } else if ("t".equals(xml.getLocalName()) && atual != null) {
                        atual.append(xml.getElementText());
                    }
                } else if (evento == XMLStreamConstants.END_ELEMENT
                        && "si".equals(xml.getLocalName()) && atual != null) {
                    textos.add(atual.toString());
                    atual = null;
                }
            }
            xml.close();
        }
        return textos;
    }

    private static void lerPlanilha(InputStream fluxo, List<String> textos,
                                    Consumer<List<String>> aoLerLinha) throws XMLStreamException {
        XMLStreamReader xml = fabrica().createXMLStreamReader(fluxo);
        List<String> linha = null;
        int coluna = -1;
        String tipo = null;
        String valor = null;

        while (xml.hasNext()) {
            int evento = xml.next();
            if (evento == XMLStreamConstants.START_ELEMENT) {
                switch (xml.getLocalName()) {
                    case "row" -> linha = new ArrayList<>();
                    case "c" -> {
                        coluna = indiceDaColuna(xml.getAttributeValue(null, "r"),
                                linha == null ? 0 : linha.size());
                        tipo = xml.getAttributeValue(null, "t");
                        valor = null;
                    }
                    case "v" -> valor = xml.getElementText();
                    case "t" -> valor = xml.getElementText();   // texto inline (<is><t>)
                    default -> { }
                }
            } else if (evento == XMLStreamConstants.END_ELEMENT) {
                switch (xml.getLocalName()) {
                    case "c" -> {
                        if (linha != null && coluna >= 0) {
                            while (linha.size() <= coluna) {
                                linha.add("");
                            }
                            linha.set(coluna, interpretar(tipo, valor, textos));
                        }
                    }
                    case "row" -> {
                        if (linha != null) {
                            aoLerLinha.accept(linha);
                        }
                        linha = null;
                    }
                    default -> { }
                }
            }
        }
        xml.close();
    }

    private static String interpretar(String tipo, String valor, List<String> textos) {
        if (valor == null) {
            return "";
        }
        if ("s".equals(tipo)) {
            int indice = Integer.parseInt(valor.trim());
            return indice < textos.size() ? textos.get(indice) : "";
        }
        return valor;
    }

    /** "AB12" vira 27 (zero-based). Sem referencia, a celula e a proxima da linha. */
    static int indiceDaColuna(String referencia, int proxima) {
        if (referencia == null || referencia.isEmpty()) {
            return proxima;
        }
        int indice = 0;
        for (char letra : referencia.toCharArray()) {
            if (letra < 'A' || letra > 'Z') {
                break;
            }
            indice = indice * 26 + (letra - 'A' + 1);
        }
        return indice - 1;
    }

    private static XMLInputFactory fabrica() {
        XMLInputFactory fabrica = XMLInputFactory.newFactory();
        // Arquivo vindo de fora: nada de DTD nem entidade externa (XXE).
        fabrica.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        fabrica.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return fabrica;
    }
}
