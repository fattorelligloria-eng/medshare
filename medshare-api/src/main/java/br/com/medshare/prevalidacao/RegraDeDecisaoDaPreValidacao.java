package br.com.medshare.prevalidacao;

import org.springframework.stereotype.Component;

/**
 * RN10 - a IA nunca aprova nem recusa sozinha.
 *
 * Esta classe existe para deixar a regra em um lugar so, legivel de cima a
 * baixo, e testavel sem banco, sem rede e sem Spring. Ela decide apenas entre
 * "seguir o fluxo" e "chamar um humano" — repare que recusar nao e uma opcao
 * aqui: a unica coisa que o automatico pode fazer sozinho e deixar passar o
 * caso obviamente correto. Qualquer outro caminho passa por uma pessoa.
 *
 * O caso feliz exige, ao mesmo tempo:
 *   - a caixa parecer lacrada,
 *   - a IA estar com certeza alta,
 *   - ter conseguido ler os tres campos (codigo de barras, lote e validade),
 *   - e nenhum deles divergir do que o doador digitou.
 *
 * Basta uma dessas condicoes falhar para a doacao ir para a central.
 */
@Component
public class RegraDeDecisaoDaPreValidacao {

    public DecisaoDaPreValidacao decidirSobre(AnalisePreValidacao analise) {
        return podeSeguirSemHumano(analise)
                ? DecisaoDaPreValidacao.SEGUIR
                : DecisaoDaPreValidacao.ENVIAR_PARA_CENTRAL;
    }

    private boolean podeSeguirSemHumano(AnalisePreValidacao analise) {
        return analise.getClasseEmbalagem() == ClasseEmbalagem.LACRADA
                && analise.getCerteza().suficienteParaDecidirSozinha()
                && analise.leuTodosOsCampos()
                && !analise.temDivergencia();
    }

    /** Texto que explica ao doador e ao analista por que o caso subiu. */
    public String explicarEncaminhamento(AnalisePreValidacao analise) {
        if (analise.getClasseEmbalagem() != ClasseEmbalagem.LACRADA) {
            return "a leitura classificou a embalagem como %s"
                    .formatted(analise.getClasseEmbalagem().porExtenso());
        }
        if (!analise.getCerteza().suficienteParaDecidirSozinha()) {
            return "a leitura ficou com certeza %s"
                    .formatted(analise.getCerteza().porExtenso());
        }
        if (!analise.leuTodosOsCampos()) {
            return "a foto não permitiu ler todos os campos da embalagem";
        }
        return String.join("; ", analise.getDivergencias());
    }
}
