package br.com.medshare.reserva;

import br.com.medshare.comum.PropriedadesDoMedShare;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.doacao.DoacaoRepository;
import br.com.medshare.doacao.ServicoDeDoacao;
import br.com.medshare.necessidade.ServicoDeNecessidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * O que precisa acontecer sozinho, sem ninguem clicar em nada. Cada rotina cita
 * o caso de uso do documento de modelagem que a pede.
 *
 * Os horarios sao de Brasilia, independentemente do fuso do servidor.
 */
@Component
public class RotinaDeManutencao {

    private static final Logger log = LoggerFactory.getLogger(RotinaDeManutencao.class);
    private static final String FUSO = "America/Sao_Paulo";

    private final ServicoDeReserva reservas;
    private final ServicoDeOferta ofertas;
    private final ServicoDeDoacao servicoDeDoacao;
    private final ServicoDeNecessidade necessidades;
    private final DoacaoRepository doacoes;
    private final PropriedadesDoMedShare propriedades;

    public RotinaDeManutencao(ServicoDeReserva reservas, ServicoDeOferta ofertas,
                              ServicoDeDoacao servicoDeDoacao, ServicoDeNecessidade necessidades,
                              DoacaoRepository doacoes, PropriedadesDoMedShare propriedades) {
        this.reservas = reservas;
        this.ofertas = ofertas;
        this.servicoDeDoacao = servicoDeDoacao;
        this.necessidades = necessidades;
        this.doacoes = doacoes;
        this.propriedades = propriedades;
    }

    /** UC05 A1 - oferta sem resposta em 24 h passa para a proxima pessoa. */
    @Scheduled(cron = "0 */10 * * * *", zone = FUSO)
    public void expirarOfertas() {
        registrar("ofertas expiradas", ofertas.expirarVencidas());
    }

    /** UC06 A3 - reserva nao retirada no prazo volta ao estoque. */
    @Scheduled(cron = "0 5 * * * *", zone = FUSO)
    public void devolverReservasVencidas() {
        reservas.liberarReservasVencidas();
    }

    /**
     * UC05 A2 - rede de seguranca do matching: caixa em estoque sem oferta
     * aberta (ninguem apto na hora, CadUnico confirmado depois, etc.) e
     * oferecida de novo.
     */
    @Scheduled(cron = "0 15 * * * *", zone = FUSO)
    @Transactional
    public void reofertarEstoqueParado() {
        List<Doacao> semOferta = doacoes.disponiveisSemOferta();
        long ofertadas = semOferta.stream().filter(d -> ofertas.ofertarDoacao(d).isPresent()).count();
        registrar("caixas ofertadas pela rotina", (int) ofertadas);
    }

    /** UC04 A2 - tenta de novo as consultas de NIS que pegaram o Portal fora do ar. */
    @Scheduled(cron = "0 20 * * * *", zone = FUSO)
    public void reconsultarCadUnico() {
        registrar("consultas de NIS refeitas", necessidades.reconsultarPendentes());
    }

    /** UC10 A3 - caso parado na central ha mais de 48 h alerta o admin. */
    @Scheduled(cron = "0 25 * * * *", zone = FUSO)
    public void alertarCentral() {
        registrar("casos atrasados na central", servicoDeDoacao.alertarCentralSobreAtrasos());
    }

    /** UC02 passo 3 - lembrete da entrega, na vespera, no fim da tarde. */
    @Scheduled(cron = "0 0 18 * * *", zone = FUSO)
    public void lembrarEntregasDeAmanha() {
        registrar("lembretes de entrega", servicoDeDoacao.enviarLembretesDaVespera());
    }

    /** UC02 A2 - agendamento sem comparecimento em 7 dias e cancelado. */
    @Scheduled(cron = "0 0 3 * * *", zone = FUSO)
    public void cancelarFaltas() {
        registrar("agendamentos cancelados por falta", servicoDeDoacao.cancelarFaltas());
    }

    /**
     * RN02 - caixa parada que nao alcanca mais os 30 dias sai do estoque.
     * Descartar aqui evita o pior cenario: alguem reservar, atravessar a cidade
     * e descobrir no balcao que o remedio nao serve mais.
     */
    @Scheduled(cron = "0 30 3 * * *", zone = FUSO)
    @Transactional
    public void descartarCaixasSemPrazo() {
        LocalDate validadeMinima = LocalDate.now()
                .plusDays(propriedades.doacao().diasMinimosDeValidade());

        List<Doacao> vencendo = doacoes.disponiveisVencendo(validadeMinima);
        for (Doacao doacao : vencendo) {
            ofertas.cancelarPendenteDa(doacao);
            doacao.descartar("validade abaixo do mínimo de %d dias"
                    .formatted(propriedades.doacao().diasMinimosDeValidade()), null);
        }
        registrar("caixas descartadas por validade", vencendo.size());
    }

    /** RN02 / UC07 A2 - reserva cuja caixa perdeu a validade minima e desfeita, com prioridade na fila. */
    @Scheduled(cron = "0 40 3 * * *", zone = FUSO)
    public void desfazerReservasSemPrazo() {
        registrar("reservas desfeitas por validade", reservas.descartarReservasSemValidade());
    }

    private void registrar(String oque, int quantidade) {
        if (quantidade > 0) {
            log.info("Rotina: {} {}", quantidade, oque);
        }
    }
}
