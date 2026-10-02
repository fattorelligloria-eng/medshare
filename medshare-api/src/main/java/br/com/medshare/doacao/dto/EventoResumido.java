package br.com.medshare.doacao.dto;

import br.com.medshare.doacao.EventoHistorico;
import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.Usuario;

import java.time.OffsetDateTime;

/**
 * Um passo do historico, do jeito que ele pode ser mostrado.
 *
 * O campo {@code responsavel} nao e o nome cru de quem registrou o evento, e
 * sim um rotulo seguro. A diferenca importa por causa da RN05: doador e
 * beneficiario nao se identificam. Mostrar "Entrega registrada - Joana Silva"
 * no historico que o doador abre quebraria a regra pela porta dos fundos,
 * mesmo com todo o resto da tela correto.
 *
 * Por isso o rotulo vem do papel, nao da pessoa:
 *   farmaceutico -> nome, porque ele age na condicao profissional dele e e
 *                   quem responde pela conferencia perante o CRF;
 *   central      -> "Equipe MedShare", porque quem decide e o servico, e o
 *                   nome do analista nao acrescenta nada a quem le;
 *   proprio      -> "Você", quando quem registrou foi quem esta olhando;
 *   beneficiario -> "Beneficiário", nunca o nome;
 *   automatico   -> nulo, e a tela escreve "Sistema".
 */
public record EventoResumido(
        OffsetDateTime quando,
        String tipo,
        String descricao,
        String statusAnterior,
        String statusNovo,
        String responsavel
) {

    public static EventoResumido de(EventoHistorico evento, Usuario quemVe) {
        return new EventoResumido(
                evento.getOcorridoEm(),
                evento.getTipo().name(),
                evento.getDescricao(),
                evento.getStatusAnterior() == null ? null : evento.getStatusAnterior().name(),
                evento.getStatusNovo() == null ? null : evento.getStatusNovo().name(),
                rotuloDe(evento.getResponsavel(), quemVe));
    }

    /** Visivel ao pacote de proposito: e o que o teste da RN05 exercita. */
    static String rotuloDe(Usuario responsavel, Usuario quemVe) {
        if (responsavel == null) return null;

        if (quemVe != null && responsavel.getId() != null
                && responsavel.getId().equals(quemVe.getId())) {
            return "Você";
        }
        if (responsavel.temPapel(Papel.FARMACEUTICO)) {
            return "Farmacêutico(a) " + primeiroNome(responsavel.getNome());
        }
        if (responsavel.temPapel(Papel.ADMIN)) {
            return "Equipe MedShare";
        }
        if (responsavel.temPapel(Papel.BENEFICIARIO)) {
            return "Beneficiário";
        }
        return "Doador";
    }

    private static String primeiroNome(String nome) {
        if (nome == null || nome.isBlank()) return "";
        int espaco = nome.trim().indexOf(' ');
        return espaco < 0 ? nome.trim() : nome.trim().substring(0, espaco);
    }
}
