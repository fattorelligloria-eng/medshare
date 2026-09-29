package br.com.medshare.reserva;

import br.com.medshare.comum.*;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.farmacia.Farmaceutico;
import br.com.medshare.farmacia.FarmaceuticoRepository;
import br.com.medshare.necessidade.*;
import br.com.medshare.notificacao.ServicoDeNotificacao;
import br.com.medshare.notificacao.TipoNotificacao;
import br.com.medshare.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Da reserva ate a entrega (UC06 e UC07).
 *
 * A reserva nasce do aceite de uma oferta (ServicoDeOferta). Aqui ficam a
 * retirada no balcao, a entrega negada, o cancelamento e as rotinas que
 * devolvem ao estoque o que nao foi retirado — sempre oferecendo a caixa para
 * a proxima pessoa da fila.
 */
@Service
public class ServicoDeReserva {

    private static final Logger log = LoggerFactory.getLogger(ServicoDeReserva.class);

    private final ReservaRepository reservas;
    private final EntregaRepository entregas;
    private final ProcuradorRepository procuradores;
    private final FarmaceuticoRepository farmaceuticos;
    private final ServicoDeOferta ofertas;
    private final ServicoDeNotificacao notificacoes;
    private final PropriedadesDoMedShare propriedades;

    public ServicoDeReserva(ReservaRepository reservas, EntregaRepository entregas,
                            ProcuradorRepository procuradores, FarmaceuticoRepository farmaceuticos,
                            ServicoDeOferta ofertas, ServicoDeNotificacao notificacoes,
                            PropriedadesDoMedShare propriedades) {
        this.reservas = reservas;
        this.entregas = entregas;
        this.procuradores = procuradores;
        this.farmaceuticos = farmaceuticos;
        this.ofertas = ofertas;
        this.notificacoes = notificacoes;
        this.propriedades = propriedades;
    }

    /**
     * UC07 - a retirada no balcao.
     *
     * RN03: o farmaceutico confere receita e documento; sem as duas
     * conferencias o objeto Entrega nem chega a ser construido.
     * RN02: a validade minima vale no momento da retirada (A2).
     * A3: quem retira e o titular ou um procurador cadastrado antes.
     */
    @Transactional(noRollbackFor = ValidadeInsuficienteNaRetirada.class)
    public Entrega registrarRetirada(String codigoRetirada, boolean receitaConferida,
                                     boolean documentoConferido, String cpfDeQuemRetira,
                                     Usuario usuarioFarmaceutico) {
        Reserva reserva = buscarNoBalcao(codigoRetirada, usuarioFarmaceutico);
        Farmaceutico farmaceutico = buscarFarmaceutico(usuarioFarmaceutico);

        if (reserva.getStatus() != StatusReserva.ATIVA) {
            throw new RegraDeNegocioViolada("RESERVA",
                    "Esta reserva está %s e não pode ser entregue".formatted(reserva.getStatus()));
        }
        if (reserva.venceu()) {
            throw new RegraDeNegocioViolada("RESERVA",
                    "Esta reserva venceu em %s. O beneficiário volta para a fila.".formatted(reserva.getExpiraEm()));
        }

        Doacao doacao = reserva.getDoacao();
        int diasMinimos = propriedades.doacao().diasMinimosDeValidade();
        if (!doacao.aindaTemValidadeSuficiente(diasMinimos)) {
            descartarPorValidade(reserva, usuarioFarmaceutico);
            throw new ValidadeInsuficienteNaRetirada(doacao.diasAteVencer(), diasMinimos);
        }

        Usuario titular = reserva.getNecessidade().getBeneficiario();
        String cpf = cpfDeQuemRetira == null || cpfDeQuemRetira.isBlank()
                ? titular.getCpf() : cpfDeQuemRetira.replaceAll("\\D", "");
        boolean porProcurador = !cpf.equals(titular.getCpf());
        if (porProcurador && !procuradores.existsByBeneficiarioIdAndCpf(titular.getId(), cpf)) {
            throw new RegraDeNegocioViolada("RN03",
                    "Esta pessoa não está cadastrada como procuradora do beneficiário. "
                            + "Só o titular ou um procurador cadastrado no app pode retirar.");
        }

        Entrega entrega = entregas.save(new Entrega(reserva, farmaceutico,
                receitaConferida, documentoConferido, cpf, porProcurador));

        reserva.concluir();
        doacao.entregar(usuarioFarmaceutico);
        reserva.getNecessidade().encerrar();

        avisarOsDoisLadosSemSeApresentarem(reserva);
        return entrega;
    }

    /**
     * UC07 A1 - a receita nao bate com o principio ativo: a entrega e negada,
     * a reserva cai, a caixa volta ao estoque (e a proxima pessoa recebe a
     * oferta) e o pedido fica em revisao ate chegar uma receita nova.
     */
    @Transactional
    public Reserva negarEntrega(String codigoRetirada, String motivo, Usuario usuarioFarmaceutico) {
        Reserva reserva = buscarNoBalcao(codigoRetirada, usuarioFarmaceutico);
        reserva.cancelar();
        reserva.getDoacao().liberarReservaCancelada("Entrega negada no balcão: " + motivo, usuarioFarmaceutico);
        reserva.getNecessidade().marcarParaRevisao(motivo);

        notificacoes.avisar(reserva.getNecessidade().getBeneficiario(), TipoNotificacao.ENTREGA_NEGADA,
                "Retirada não concluída",
                "A farmácia não pôde entregar %s: %s. Envie uma receita atualizada no app para voltar à fila."
                        .formatted(reserva.getDoacao().getMedicamento().getNomeComercial(), motivo));
        ofertas.ofertarDoacao(reserva.getDoacao());
        return reserva;
    }

    @Transactional
    public Reserva cancelar(String codigoRetirada, Usuario solicitante) {
        Reserva reserva = reservas.findByCodigoRetirada(codigoRetirada)
                .orElseThrow(() -> new RecursoNaoEncontrado("Código de retirada", codigoRetirada));
        if (!reserva.getNecessidade().getBeneficiario().getId().equals(solicitante.getId())) {
            throw new RecursoNaoEncontrado("Código de retirada", codigoRetirada);
        }

        reserva.cancelar();
        reserva.getDoacao().liberarReservaCancelada(solicitante);
        ofertas.ofertarDoacao(reserva.getDoacao());
        return reserva;
    }

    /**
     * RN03 - o que o farmaceutico precisa ver para conferir a retirada: quem e
     * o titular e a receita anexada. So a farmacia onde a caixa esta enxerga isso.
     */
    @Transactional(readOnly = true)
    public Reserva buscarNoBalcao(String codigoRetirada, Usuario usuarioFarmaceutico) {
        Reserva reserva = reservas.findByCodigoRetirada(codigoRetirada)
                .orElseThrow(() -> new RecursoNaoEncontrado("Código de retirada", codigoRetirada));
        Farmaceutico farmaceutico = buscarFarmaceutico(usuarioFarmaceutico);
        var ponto = reserva.getDoacao().getPontoDeColeta();
        if (ponto == null || !farmaceutico.atuaEm(ponto)) {
            throw new RegraDeNegocioViolada("RN10",
                    "Esta reserva é para retirada em outro ponto de coleta");
        }
        return reserva;
    }

    @Transactional(readOnly = true)
    public List<Procurador> procuradoresDe(Reserva reserva) {
        return procuradores.findByBeneficiarioIdOrderByNome(
                reserva.getNecessidade().getBeneficiario().getId());
    }

    // --- rotinas --------------------------------------------------------------

    /**
     * UC06 A3 - reserva nao retirada no prazo volta ao estoque, e a caixa e
     * oferecida a proxima pessoa. O beneficiario continua na fila.
     */
    @Transactional
    public int liberarReservasVencidas() {
        List<Reserva> vencidas = reservas.findByStatusAndExpiraEmBefore(
                StatusReserva.ATIVA, OffsetDateTime.now());

        for (Reserva reserva : vencidas) {
            reserva.expirar();
            reserva.getDoacao().liberarReservaExpirada();
            notificacoes.avisar(reserva.getNecessidade().getBeneficiario(),
                    TipoNotificacao.RESERVA_EXPIRADA,
                    "Reserva expirada",
                    "O prazo para retirar %s terminou. Você continua na fila."
                            .formatted(reserva.getDoacao().getMedicamento().getNomeComercial()));
        }
        vencidas.forEach(reserva -> ofertas.ofertarDoacao(reserva.getDoacao()));
        if (!vencidas.isEmpty()) {
            log.info("{} reserva(s) expirada(s) devolvida(s) ao estoque", vencidas.size());
        }
        return vencidas.size();
    }

    /**
     * RN02 / UC07 A2 antes de a pessoa sair de casa: reserva cuja caixa ja nao
     * alcanca a validade minima e desfeita, a caixa e descartada e o
     * beneficiario volta a fila com prioridade.
     */
    @Transactional
    public int descartarReservasSemValidade() {
        LocalDate validadeMinima = LocalDate.now().plusDays(propriedades.doacao().diasMinimosDeValidade());
        List<Reserva> afetadas = reservas.ativasComValidadeAbaixoDe(validadeMinima);
        afetadas.forEach(reserva -> descartarPorValidade(reserva, null));
        return afetadas.size();
    }

    private void descartarPorValidade(Reserva reserva, Usuario responsavel) {
        int diasMinimos = propriedades.doacao().diasMinimosDeValidade();
        reserva.cancelar();
        reserva.getDoacao().descartar(
                "validade abaixo do mínimo de %d dias na data da retirada".formatted(diasMinimos), responsavel);
        Necessidade necessidade = reserva.getNecessidade();
        necessidade.priorizar();

        notificacoes.avisar(necessidade.getBeneficiario(), TipoNotificacao.RESERVA_EXPIRADA,
                "Reserva cancelada pela validade",
                "A caixa de %s reservada para você chegou perto do vencimento e não pode ser entregue. "
                        .formatted(reserva.getDoacao().getMedicamento().getNomeComercial())
                        + "Você voltou para a fila com prioridade.");
        ofertas.ofertarParaNecessidade(necessidade);
    }

    /**
     * RN05 - os dois recebem aviso, nenhum recebe o nome do outro.
     */
    private void avisarOsDoisLadosSemSeApresentarem(Reserva reserva) {
        String medicamento = reserva.getDoacao().getMedicamento().getNomeComercial();

        notificacoes.avisar(reserva.getDoacao().getDoador(), TipoNotificacao.ENTREGA_CONCLUIDA,
                "Sua doação chegou a quem precisava",
                "%s foi entregue hoje. Obrigado por doar.".formatted(medicamento));

        notificacoes.avisar(reserva.getNecessidade().getBeneficiario(),
                TipoNotificacao.ENTREGA_CONCLUIDA,
                "Retirada concluída",
                "Você retirou %s. Guarde a caixa conforme a orientação do farmacêutico."
                        .formatted(medicamento));
    }

    private Farmaceutico buscarFarmaceutico(Usuario usuario) {
        return farmaceuticos.findById(usuario.getId())
                .orElseThrow(() -> new RegraDeNegocioViolada("RN10",
                        "Somente um farmacêutico cadastrado pode registrar a entrega"));
    }
}
