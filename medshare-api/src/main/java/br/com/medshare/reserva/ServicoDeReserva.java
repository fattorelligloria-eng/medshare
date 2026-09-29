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

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Da reserva ate a entrega.
 *
 * Aqui se cruzam quatro regras, e a ordem em que sao conferidas importa: RN08
 * (o beneficiario esta no CadUnico), RN03 (tem receita valida), RN04 (nao tem
 * outra reserva ativa do mesmo medicamento) e RN02 (a caixa ainda tem prazo).
 * Conferimos tudo antes de prender a caixa, para nao tirar do estoque um
 * medicamento que a pessoa nao conseguiria retirar.
 */
@Service
public class ServicoDeReserva {

    private static final Logger log = LoggerFactory.getLogger(ServicoDeReserva.class);

    private final ReservaRepository reservas;
    private final EntregaRepository entregas;
    private final NecessidadeRepository pedidos;
    private final ServicoDeNecessidade necessidades;
    private final VerificacaoCadUnicoRepository verificacoes;
    private final FarmaceuticoRepository farmaceuticos;
    private final ServicoDeMatching matching;
    private final GeradorDeCodigo codigos;
    private final ServicoDeNotificacao notificacoes;
    private final PropriedadesDoMedShare propriedades;

    public ServicoDeReserva(ReservaRepository reservas, EntregaRepository entregas,
                            NecessidadeRepository pedidos, ServicoDeNecessidade necessidades,
                            VerificacaoCadUnicoRepository verificacoes,
                            FarmaceuticoRepository farmaceuticos, ServicoDeMatching matching,
                            GeradorDeCodigo codigos, ServicoDeNotificacao notificacoes,
                            PropriedadesDoMedShare propriedades) {
        this.reservas = reservas;
        this.entregas = entregas;
        this.pedidos = pedidos;
        this.necessidades = necessidades;
        this.verificacoes = verificacoes;
        this.farmaceuticos = farmaceuticos;
        this.matching = matching;
        this.codigos = codigos;
        this.notificacoes = notificacoes;
        this.propriedades = propriedades;
    }

    @Transactional
    public Reserva reservarPara(Long necessidadeId, Usuario beneficiario) {
        Necessidade necessidade = pedidos.findById(necessidadeId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Necessidade", necessidadeId));

        exigirQueSejaODono(necessidade, beneficiario);
        exigirNecessidadeAtiva(necessidade);          // RN04
        exigirCadUnicoVigente(beneficiario);          // RN08
        necessidade.exigirReceitaValida();            // RN03
        exigirQueNaoTenhaReservaAtiva(necessidade);   // RN04

        Doacao doacao = matching.melhorDoacaoPara(necessidade)
                .orElseThrow(() -> new RegraDeNegocioViolada("ESTOQUE",
                        "Ainda não há %s disponível. Você está na fila e será avisado."
                                .formatted(necessidade.getMedicamento().getNomeComercial())));

        Reserva reserva = new Reserva(codigos.paraRetirada(), doacao, necessidade,
                propriedades.reserva().horasParaRetirada());
        doacao.reservar(beneficiario);
        reservas.save(reserva);

        notificacoes.avisar(beneficiario, TipoNotificacao.RESERVA_CRIADA,
                "Medicamento reservado",
                "Retire %s em %s até %s. Código: %s. Leve documento e a receita."
                        .formatted(necessidade.getMedicamento().getNomeComercial(),
                                doacao.getPontoDeColeta().getNome(),
                                reserva.getExpiraEm().toLocalDate(),
                                reserva.getCodigoRetirada()));
        return reserva;
    }

    /**
     * A retirada no balcao. RN03 - o farmaceutico confere a receita e o
     * documento antes de entregar; sem as duas conferencias, o objeto Entrega
     * nem chega a ser construido.
     */
    @Transactional
    public Entrega registrarRetirada(String codigoRetirada, boolean receitaConferida,
                                     boolean documentoConferido, Usuario usuarioFarmaceutico) {
        Reserva reserva = buscarNoBalcao(codigoRetirada, usuarioFarmaceutico);
        Farmaceutico farmaceutico = buscarFarmaceutico(usuarioFarmaceutico);

        if (reserva.venceu()) {
            throw new RegraDeNegocioViolada("RESERVA",
                    "Esta reserva venceu em %s. Peça ao beneficiário para reservar de novo."
                            .formatted(reserva.getExpiraEm()));
        }

        Entrega entrega = entregas.save(
                new Entrega(reserva, farmaceutico, receitaConferida, documentoConferido));

        reserva.concluir();
        reserva.getDoacao().entregar(usuarioFarmaceutico);
        reserva.getNecessidade().encerrar();

        avisarOsDoisLadosSemSeApresentarem(reserva);
        return entrega;
    }

    @Transactional
    public Reserva cancelar(String codigoRetirada, Usuario solicitante) {
        Reserva reserva = reservas.findByCodigoRetirada(codigoRetirada)
                .orElseThrow(() -> new RecursoNaoEncontrado("Código de retirada", codigoRetirada));
        exigirQueSejaODono(reserva.getNecessidade(), solicitante);

        reserva.cancelar();
        reserva.getDoacao().liberarReservaCancelada(solicitante);
        necessidades.avisarFilaDeEspera(reserva.getDoacao().getMedicamento());
        return reserva;
    }

    /**
     * RN03 - o que o farmaceutico precisa ver para conferir a retirada: quem e
     * o titular (para comparar com o documento) e a receita anexada. So a
     * farmacia onde a caixa esta enxerga isso.
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

    /**
     * Devolve ao estoque o que ninguem retirou no prazo.
     * Roda de hora em hora: uma caixa presa numa reserva abandonada e uma caixa
     * que alguem da fila poderia estar usando.
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
                    "O prazo para retirar %s terminou. Voce pode reservar novamente."
                            .formatted(reserva.getDoacao().getMedicamento().getNomeComercial()));
            necessidades.avisarFilaDeEspera(reserva.getDoacao().getMedicamento());
        }
        if (!vencidas.isEmpty()) {
            log.info("{} reserva(s) expirada(s) devolvida(s) ao estoque", vencidas.size());
        }
        return vencidas.size();
    }

    /**
     * RN05 - os dois recebem aviso, nenhum recebe o nome do outro.
     * O doador fica sabendo que a doacao dele foi usada; o beneficiario, que a
     * retirada foi concluida. Nenhuma das duas mensagens cita a outra pessoa.
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

    // --- guardas -------------------------------------------------------------

    private Farmaceutico buscarFarmaceutico(Usuario usuario) {
        return farmaceuticos.findById(usuario.getId())
                .orElseThrow(() -> new RegraDeNegocioViolada("RN10",
                        "Somente um farmacêutico cadastrado pode registrar a entrega"));
    }

    /**
     * RN04 - pedido encerrado (ja atendido ou cancelado) nao reserva de novo.
     * Sem isto, o id de um pedido ja entregue, com a mesma receita, servia
     * para tirar outra caixa do estoque.
     */
    private void exigirNecessidadeAtiva(Necessidade necessidade) {
        if (!necessidade.isAtiva()) {
            throw new RegraDeNegocioViolada("RN04",
                    "Este pedido já foi encerrado. Faça um novo pedido se ainda precisar do medicamento.");
        }
    }

    private void exigirQueSejaODono(Necessidade necessidade, Usuario usuario) {
        if (!necessidade.getBeneficiario().getId().equals(usuario.getId())) {
            throw new RegraDeNegocioViolada("RN05", "Este pedido pertence a outra pessoa");
        }
    }

    /** RN08 - sem CadUnico vigente nao ha reserva. */
    private void exigirCadUnicoVigente(Usuario beneficiario) {
        boolean vigente = verificacoes
                .findFirstByUsuarioIdOrderByValidoAteDesc(beneficiario.getId())
                .map(VerificacaoCadUnico::estaVigente)
                .orElse(false);

        if (!vigente) {
            throw new RegraDeNegocioViolada("RN08",
                    "Para receber medicamentos é preciso ter NIS ativo no CadÚnico. "
                            + "Informe seu NIS no aplicativo para fazermos a verificação.");
        }
    }

    /** RN04 - uma reserva ativa por medicamento por beneficiario. */
    private void exigirQueNaoTenhaReservaAtiva(Necessidade necessidade) {
        boolean jaTem = reservas
                .findByNecessidadeBeneficiarioIdOrderByCriadaEmDesc(
                        necessidade.getBeneficiario().getId())
                .stream()
                .anyMatch(reserva -> reserva.getStatus() == StatusReserva.ATIVA
                        && reserva.getNecessidade().getMedicamento().getId()
                                .equals(necessidade.getMedicamento().getId()));

        if (jaTem) {
            throw new RegraDeNegocioViolada("RN04",
                    "Você já tem uma reserva ativa de %s. Retire ou cancele antes de reservar outra."
                            .formatted(necessidade.getMedicamento().getNomeComercial()));
        }
    }
}
