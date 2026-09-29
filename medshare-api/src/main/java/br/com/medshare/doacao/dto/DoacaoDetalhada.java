package br.com.medshare.doacao.dto;

import br.com.medshare.doacao.Agendamento;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.doacao.EventoHistorico;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * RN06 - a doacao com o historico completo, do cadastro ate agora.
 *
 * O agendamento atual traz o codigo de entrega que o doador mostra no balcao
 * (UC03 passo 1). {@code podeReagendar}: o agendamento foi cancelado e o
 * reagendamento unico ainda nao foi usado (UC02 A2).
 */
public record DoacaoDetalhada(
        DoacaoResumida doacao,
        AgendamentoAtual agendamento,
        boolean podeReagendar,
        List<EventoResumido> historico
) {

    public record AgendamentoAtual(String pontoDeColeta, String endereco, OffsetDateTime dataHora,
                                   String codigoEntrega) {
        static AgendamentoAtual de(Agendamento a) {
            var endereco = a.getPontoDeColeta().getEndereco();
            return new AgendamentoAtual(a.getPontoDeColeta().getNome(),
                    "%s, %s - %s".formatted(endereco.getLogradouro(), endereco.getNumero(), endereco.getBairro()),
                    a.getDataHora(), a.getCodigoEntrega());
        }
    }

    public static DoacaoDetalhada de(Doacao doacao, List<EventoHistorico> eventos,
                                     Agendamento agendamento, boolean podeReagendar) {
        return new DoacaoDetalhada(
                DoacaoResumida.de(doacao),
                agendamento == null ? null : AgendamentoAtual.de(agendamento),
                podeReagendar,
                eventos.stream().map(EventoResumido::de).toList());
    }

    public static DoacaoDetalhada de(Doacao doacao, List<EventoHistorico> eventos) {
        return de(doacao, eventos, null, false);
    }
}
