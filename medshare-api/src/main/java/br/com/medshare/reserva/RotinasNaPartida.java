package br.com.medshare.reserva;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Roda, assim que a API sobe, as rotinas que so tem horario de madrugada.
 *
 * Cancelar faltas, descartar caixa sem prazo e desfazer reserva sem prazo
 * rodam as 3h. Com a API ligada so durante o dia — que e como ela roda no
 * desenvolvimento e na apresentacao — as 3h nunca chegavam, e um agendamento
 * de duas semanas atras continuava na tela como "seu proximo passo".
 *
 * As tres so mexem no que ja venceu, entao rodar a mais nao muda nada. Fica
 * numa classe separada para chamar a RotinaDeManutencao pelo proxy do Spring:
 * chamada de dentro da propria classe pularia o @Transactional, e o descarte
 * nao seria gravado.
 */
@Component
public class RotinasNaPartida {

    private static final Logger log = LoggerFactory.getLogger(RotinasNaPartida.class);

    private final RotinaDeManutencao rotina;

    public RotinasNaPartida(RotinaDeManutencao rotina) {
        this.rotina = rotina;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void colocarEmDia() {
        rodar("cancelar faltas", rotina::cancelarFaltas);
        rodar("descartar caixas sem prazo", rotina::descartarCaixasSemPrazo);
        rodar("desfazer reservas sem prazo", rotina::desfazerReservasSemPrazo);
    }

    /** Uma rotina com problema nao derruba a partida nem impede as outras. */
    private void rodar(String nome, Runnable passo) {
        try {
            passo.run();
        } catch (RuntimeException e) {
            log.error("Rotina de partida '{}' falhou; ela roda de novo no horario dela", nome, e);
        }
    }
}
