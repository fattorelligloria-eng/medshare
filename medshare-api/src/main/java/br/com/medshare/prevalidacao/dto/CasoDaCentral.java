package br.com.medshare.prevalidacao.dto;

import br.com.medshare.doacao.Doacao;
import br.com.medshare.prevalidacao.AnalisePreValidacao;

import java.time.LocalDate;
import java.util.List;

/**
 * Um caso na fila da central (RN10), com tudo que o analista precisa para
 * decidir em uma tela so: o que o doador declarou, o que a IA leu, e onde as
 * duas versoes divergem.
 */
public record CasoDaCentral(
        String codigo,
        String medicamento,
        String fotoUrl,
        String loteDeclarado,
        LocalDate validadeDeclarada,
        String eanEsperado,

        String loteLido,
        LocalDate validadeLida,
        String eanLido,
        String classeEmbalagem,
        String certeza,
        String motivo,
        String avaliador,
        List<String> divergencias
) {

    public static CasoDaCentral de(Doacao doacao, AnalisePreValidacao analise) {
        return new CasoDaCentral(
                doacao.getCodigo(),
                doacao.getMedicamento().descricaoCompleta(),
                doacao.getFotoUrl(),
                doacao.getLote(),
                doacao.getValidade(),
                doacao.getMedicamento().getEan(),
                analise == null ? null : analise.getLoteLido(),
                analise == null ? null : analise.getValidadeLida(),
                analise == null ? null : analise.getEanLido(),
                analise == null ? null : analise.getClasseEmbalagem().name(),
                analise == null ? null : analise.getCerteza().name(),
                analise == null ? null : analise.getMotivo(),
                analise == null ? null : analise.getAvaliador(),
                analise == null ? List.of() : analise.getDivergencias());
    }
}
