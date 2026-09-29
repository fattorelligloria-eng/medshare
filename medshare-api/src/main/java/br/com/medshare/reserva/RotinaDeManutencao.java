package br.com.medshare.reserva;

import br.com.medshare.comum.PropriedadesDoMedShare;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.doacao.DoacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Duas coisas que precisam acontecer sozinhas, sem ninguem clicar em nada:
 * devolver ao estoque reservas abandonadas e tirar de circulacao as caixas que
 * ja nao alcancam a validade minima (RN02).
 */
@Component
public class RotinaDeManutencao {

    private static final Logger log = LoggerFactory.getLogger(RotinaDeManutencao.class);

    private final ServicoDeReserva reservas;
    private final DoacaoRepository doacoes;
    private final PropriedadesDoMedShare propriedades;

    public RotinaDeManutencao(ServicoDeReserva reservas, DoacaoRepository doacoes,
                              PropriedadesDoMedShare propriedades) {
        this.reservas = reservas;
        this.doacoes = doacoes;
        this.propriedades = propriedades;
    }

    @Scheduled(cron = "0 0 * * * *")   // de hora em hora
    public void devolverReservasVencidas() {
        reservas.liberarReservasVencidas();
    }

    /**
     * RN02 - caixa parada que nao alcanca mais os 30 dias sai do estoque.
     * Descartar aqui evita o pior cenario possivel: alguem reservar, atravessar
     * a cidade e descobrir no balcao que o remedio nao serve mais.
     */
    @Scheduled(cron = "0 30 3 * * *")  // todo dia as 3h30
    @Transactional
    public void descartarCaixasSemPrazo() {
        LocalDate validadeMinima = LocalDate.now()
                .plusDays(propriedades.doacao().diasMinimosDeValidade());

        List<Doacao> vencendo = doacoes.disponiveisVencendo(validadeMinima);
        for (Doacao doacao : vencendo) {
            doacao.descartar("validade abaixo do mínimo de %d dias"
                    .formatted(propriedades.doacao().diasMinimosDeValidade()), null);
        }
        if (!vencendo.isEmpty()) {
            log.info("{} doacao(oes) descartada(s) por validade insuficiente", vencendo.size());
        }
    }
}
