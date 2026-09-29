package br.com.medshare.reserva;

import br.com.medshare.comum.GeradorDeCodigo;
import br.com.medshare.comum.PropriedadesDoMedShare;
import br.com.medshare.comum.RecursoNaoEncontrado;
import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.necessidade.Necessidade;
import br.com.medshare.necessidade.NecessidadeRepository;
import br.com.medshare.necessidade.VerificacaoCadUnico;
import br.com.medshare.necessidade.VerificacaoCadUnicoRepository;
import br.com.medshare.notificacao.ServicoDeNotificacao;
import br.com.medshare.notificacao.TipoNotificacao;
import br.com.medshare.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * UC05 e UC06 - oferta com prazo.
 *
 * Quando uma caixa fica disponivel (ou alguem entra na fila), o sistema escolhe
 * UMA pessoa e oferece a caixa a ela, com 24 h para aceitar. Aceitou: vira
 * reserva, com 5 dias para retirar. Recusou ou deixou vencer: a caixa vai para
 * a proxima pessoa da fila. Assim ninguem precisa ficar abrindo o app para
 * "ver se chegou", e a caixa nao fica presa esperando quem nao vai buscar.
 */
@Service
public class ServicoDeOferta {

    private static final Logger log = LoggerFactory.getLogger(ServicoDeOferta.class);
    private static final Duration PRAZO_PARA_ACEITAR = Duration.ofHours(24);
    private static final DateTimeFormatter DIA_E_HORA =
            DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm").withZone(java.time.ZoneId.of("America/Sao_Paulo"));

    private final OfertaRepository ofertas;
    private final ReservaRepository reservas;
    private final NecessidadeRepository necessidades;
    private final VerificacaoCadUnicoRepository verificacoes;
    private final ServicoDeMatching matching;
    private final ServicoDeNotificacao notificacoes;
    private final GeradorDeCodigo codigos;
    private final PropriedadesDoMedShare propriedades;

    public ServicoDeOferta(OfertaRepository ofertas, ReservaRepository reservas,
                           NecessidadeRepository necessidades, VerificacaoCadUnicoRepository verificacoes,
                           ServicoDeMatching matching, ServicoDeNotificacao notificacoes,
                           GeradorDeCodigo codigos, PropriedadesDoMedShare propriedades) {
        this.ofertas = ofertas;
        this.reservas = reservas;
        this.necessidades = necessidades;
        this.verificacoes = verificacoes;
        this.matching = matching;
        this.notificacoes = notificacoes;
        this.codigos = codigos;
        this.propriedades = propriedades;
    }

    // --- gatilhos do matching (UC05) -----------------------------------------

    /** A caixa acabou de ficar disponivel: oferece ao melhor da fila. */
    @Transactional
    public Optional<Oferta> ofertarDoacao(Doacao doacao) {
        if (!doacao.estaDisponivel()
                || ofertas.findFirstByDoacaoIdAndStatus(doacao.getId(), Oferta.Status.PENDENTE).isPresent()) {
            return Optional.empty();
        }
        List<Long> jaDispensaram = ofertas.necessidadesQueJaDispensaram(doacao.getId());
        List<Necessidade> fila = necessidades.filaDoPrincipioAtivo(
                doacao.getMedicamento().getPrincipioAtivo());

        return matching.melhorNecessidadePara(doacao, fila,
                        n -> !jaDispensaram.contains(n.getId()) && podeReceberOferta(n))
                .map(necessidade -> criar(doacao, necessidade));
    }

    /** Alguem entrou na fila (ou renovou a receita): procura uma caixa para ele. */
    @Transactional
    public Optional<Oferta> ofertarParaNecessidade(Necessidade necessidade) {
        if (!necessidade.aptaParaOferta()
                || ofertas.existsByNecessidadeIdAndStatus(necessidade.getId(), Oferta.Status.PENDENTE)
                || !podeReceberOferta(necessidade)) {
            return Optional.empty();
        }
        List<Long> dispensadas = ofertas.doacoesDispensadasPor(necessidade.getId());
        return matching.melhorDoacaoPara(necessidade, dispensadas)
                .map(doacao -> criar(doacao, necessidade));
    }

    /** RN08 (CadUnico vigente) e RN04 (sem outra reserva ativa do mesmo principio ativo). */
    private boolean podeReceberOferta(Necessidade necessidade) {
        Usuario beneficiario = necessidade.getBeneficiario();
        boolean cadUnicoVigente = verificacoes
                .findFirstByUsuarioIdOrderByValidoAteDesc(beneficiario.getId())
                .map(VerificacaoCadUnico::estaVigente)
                .orElse(false);
        return cadUnicoVigente && !reservas.existeAtivaDoPrincipioAtivo(
                beneficiario.getId(), necessidade.getMedicamento().getPrincipioAtivo());
    }

    private Oferta criar(Doacao doacao, Necessidade necessidade) {
        Oferta oferta = ofertas.save(new Oferta(doacao, necessidade, PRAZO_PARA_ACEITAR));
        doacao.registrarOferta(null);
        notificacoes.avisar(necessidade.getBeneficiario(), TipoNotificacao.OFERTA_RECEBIDA,
                "Chegou %s para você".formatted(doacao.getMedicamento().getNomeComercial()),
                "Uma caixa está disponível em %s. Aceite até %s no app para reservar."
                        .formatted(doacao.getPontoDeColeta().getNome(),
                                DIA_E_HORA.format(oferta.getExpiraEm())));
        log.info("Oferta {} criada: doacao {} para a necessidade {}",
                oferta.getId(), doacao.getCodigo(), necessidade.getId());
        return oferta;
    }

    // --- resposta do beneficiario (UC06) -------------------------------------

    /** UC06 passos 2 a 4 - aceitou: vira reserva com prazo de retirada. */
    @Transactional
    public Reserva aceitar(Long ofertaId, Usuario beneficiario) {
        Oferta oferta = buscarDoBeneficiario(ofertaId, beneficiario);
        Necessidade necessidade = oferta.getNecessidade();
        Doacao doacao = oferta.getDoacao();

        necessidade.exigirReceitaValida();                                 // RN03
        if (reservas.existeAtivaDoPrincipioAtivo(beneficiario.getId(),
                doacao.getMedicamento().getPrincipioAtivo())) {                 // RN04
            throw new RegraDeNegocioViolada("RN04",
                    "Você já tem uma reserva ativa deste medicamento. Retire ou cancele antes.");
        }

        oferta.aceitar();
        Reserva reserva = new Reserva(codigos.paraRetirada(), doacao, necessidade,
                propriedades.reserva().horasParaRetirada());
        doacao.reservar(beneficiario);
        reservas.save(reserva);

        notificacoes.avisar(beneficiario, TipoNotificacao.RESERVA_CRIADA,
                "Medicamento reservado",
                "Retire %s em %s até %s. Código: %s. Leve documento com foto e a receita."
                        .formatted(doacao.getMedicamento().getNomeComercial(),
                                doacao.getPontoDeColeta().getNome(),
                                DIA_E_HORA.format(reserva.getExpiraEm()),
                                reserva.getCodigoRetirada()));
        return reserva;
    }

    /** UC06 A1 - recusou: a caixa vai para a proxima pessoa da fila. */
    @Transactional
    public Oferta recusar(Long ofertaId, Usuario beneficiario) {
        Oferta oferta = buscarDoBeneficiario(ofertaId, beneficiario);
        oferta.recusar();
        ofertarDoacao(oferta.getDoacao());
        return oferta;
    }

    /** UC05 A1 - quem nao respondeu em 24 h perde a vez. Roda periodicamente. */
    @Transactional
    public int expirarVencidas() {
        List<Oferta> vencidas = ofertas.findByStatusAndExpiraEmBefore(
                Oferta.Status.PENDENTE, OffsetDateTime.now());
        for (Oferta oferta : vencidas) {
            oferta.expirar();
            notificacoes.avisar(oferta.getNecessidade().getBeneficiario(), TipoNotificacao.OFERTA_EXPIRADA,
                    "Oferta expirada",
                    "O prazo para aceitar %s terminou. Você continua na fila."
                            .formatted(oferta.getDoacao().getMedicamento().getNomeComercial()));
        }
        // Em um segundo passo, depois de todas encerradas: assim uma caixa nao e
        // oferecida de novo a quem tambem acabou de deixar vencer.
        vencidas.forEach(oferta -> ofertarDoacao(oferta.getDoacao()));
        return vencidas.size();
    }

    /** A caixa deixou de estar disponivel: a oferta aberta, se houver, cai. */
    @Transactional
    public void cancelarPendenteDa(Doacao doacao) {
        ofertas.findFirstByDoacaoIdAndStatus(doacao.getId(), Oferta.Status.PENDENTE)
                .ifPresent(Oferta::cancelar);
    }

    @Transactional(readOnly = true)
    public List<Oferta> doBeneficiario(Usuario beneficiario) {
        return ofertas.doBeneficiario(beneficiario.getId());
    }

    private Oferta buscarDoBeneficiario(Long ofertaId, Usuario beneficiario) {
        Oferta oferta = ofertas.findById(ofertaId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Oferta", ofertaId));
        if (!oferta.getNecessidade().getBeneficiario().getId().equals(beneficiario.getId())) {
            throw new RecursoNaoEncontrado("Oferta", ofertaId);
        }
        return oferta;
    }
}
