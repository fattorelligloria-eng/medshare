package br.com.medshare.prevalidacao.dto;

import br.com.medshare.doacao.Doacao;
import br.com.medshare.prevalidacao.AnalisePreValidacao;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
        List<String> divergencias,
        /** UC10 passo 1 - desde quando o caso espera na central. */
        OffsetDateTime esperandoDesde,
        boolean lacreDeclarado
) {

    /** @param fotoAssinada link com prazo para exibir a foto (nunca a URL gravada) */
    public static CasoDaCentral de(Doacao doacao, AnalisePreValidacao analise, String fotoAssinada) {
        return new CasoDaCentral(
                doacao.getCodigo(),
                doacao.getMedicamento().descricaoCompleta(),
                fotoAssinada,
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
                analise == null ? List.of() : analise.getDivergencias(),
                doacao.getAtualizadoEm(),
                doacao.isLacreDeclarado());
    }
}
