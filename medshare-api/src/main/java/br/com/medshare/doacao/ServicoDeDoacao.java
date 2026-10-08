package br.com.medshare.doacao;

import br.com.medshare.comum.*;
import br.com.medshare.farmacia.*;
import br.com.medshare.integracao.AvaliadorDeEmbalagem;
import br.com.medshare.integracao.LeituraDaEmbalagem;
import br.com.medshare.medicamento.Medicamento;
import br.com.medshare.medicamento.MedicamentoRepository;
import br.com.medshare.notificacao.ServicoDeNotificacao;
import br.com.medshare.notificacao.TipoNotificacao;
import br.com.medshare.prevalidacao.*;
import br.com.medshare.reserva.ServicoDeOferta;
import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.Usuario;
import br.com.medshare.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * O caminho da doacao, do cadastro ate ficar disponivel na prateleira
 * (UC01, UC02, UC03 e UC10).
 *
 * O service coordena — busca, salva, avisa. Quem sabe se uma transicao e
 * permitida e a propria Doacao; quem sabe se a IA pode decidir sozinha e a
 * RegraDeDecisaoDaPreValidacao. Aqui nao ha "if" de regra de negocio que ja
 * tenha dono no dominio.
 */
@Service
public class ServicoDeDoacao {

    private static final Logger log = LoggerFactory.getLogger(ServicoDeDoacao.class);

    /** UC01 passo 4 - quantas caixas iguais cabem num cadastro. */
    public static final int QUANTIDADE_MAXIMA = 10;
    /** UC02 A2 - sem comparecer depois deste prazo, o agendamento e cancelado. */
    private static final Duration TOLERANCIA_DE_FALTA = Duration.ofDays(7);
    /** UC10 A3 - caso parado na central ha mais que isto gera alerta. */
    private static final Duration PRAZO_DA_CENTRAL = Duration.ofHours(48);
    /** UC02 passo 1 - quantos dias a frente o app oferece horarios. */
    private static final int DIAS_DE_AGENDA = 14;

    private static final DateTimeFormatter DIA_E_HORA =
            DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm").withZone(PontoDeColeta.FUSO);

    private final DoacaoRepository doacoes;
    private final MedicamentoRepository medicamentos;
    private final PontoDeColetaRepository pontos;
    private final FarmaceuticoRepository farmaceuticos;
    private final AgendamentoRepository agendamentos;
    private final ValidacaoRepository validacoes;
    private final AnalisePreValidacaoRepository analises;
    private final RevisaoCentralRepository revisoes;
    private final UsuarioRepository usuarios;

    private final AvaliadorDeEmbalagem avaliador;
    private final RegraDeDecisaoDaPreValidacao regraDaPreValidacao;
    private final GeradorDeCodigo codigos;
    private final ServicoDeNotificacao notificacoes;
    private final ServicoDeOferta ofertas;
    private final PropriedadesDoMedShare propriedades;

    public ServicoDeDoacao(DoacaoRepository doacoes, MedicamentoRepository medicamentos,
                           PontoDeColetaRepository pontos, FarmaceuticoRepository farmaceuticos,
                           AgendamentoRepository agendamentos, ValidacaoRepository validacoes,
                           AnalisePreValidacaoRepository analises, RevisaoCentralRepository revisoes,
                           UsuarioRepository usuarios,
                           AvaliadorDeEmbalagem avaliador,
                           RegraDeDecisaoDaPreValidacao regraDaPreValidacao,
                           GeradorDeCodigo codigos, ServicoDeNotificacao notificacoes,
                           ServicoDeOferta ofertas, PropriedadesDoMedShare propriedades) {
        this.doacoes = doacoes;
        this.medicamentos = medicamentos;
        this.pontos = pontos;
        this.farmaceuticos = farmaceuticos;
        this.agendamentos = agendamentos;
        this.validacoes = validacoes;
        this.analises = analises;
        this.revisoes = revisoes;
        this.usuarios = usuarios;
        this.avaliador = avaliador;
        this.regraDaPreValidacao = regraDaPreValidacao;
        this.codigos = codigos;
        this.notificacoes = notificacoes;
        this.ofertas = ofertas;
        this.propriedades = propriedades;
    }

    // --- UC01: cadastro -------------------------------------------------------

    /** Uma caixa, com a declaracao de lacre ja feita (dados de demonstracao). */
    @Transactional
    public Doacao cadastrar(Long medicamentoId, String lote, LocalDate validade,
                            String fotoUrl, Usuario doador) {
        return cadastrar(medicamentoId, lote, validade, fotoUrl, 1, true, doador).get(0);
    }

    /**
     * UC01 - o doador cadastra as caixas pelo aplicativo.
     *
     * Cada caixa vira uma doacao propria, com codigo e historico proprios
     * (RN06): elas podem ir para beneficiarios diferentes. A foto e lida uma
     * vez so, e a mesma leitura vale para todas — sao caixas iguais do mesmo
     * lote.
     *
     * A propria criacao aplica RN01 (lacre declarado), RN07 (PMC minimo) e
     * RN02 (validade minima): se alguma falhar, nenhuma linha e gravada.
     */
    @Transactional
    public List<Doacao> cadastrar(Long medicamentoId, String lote, LocalDate validade,
                                  String fotoUrl, int quantidade, boolean lacreDeclarado,
                                  Usuario doador) {
        if (quantidade < 1 || quantidade > QUANTIDADE_MAXIMA) {
            throw new RegraDeNegocioViolada("QUANTIDADE",
                    "Cadastre de 1 a %d caixas por vez".formatted(QUANTIDADE_MAXIMA));
        }
        Medicamento medicamento = medicamentos.findById(medicamentoId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Medicamento", medicamentoId));

        List<Doacao> criadas = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            criadas.add(doacoes.save(Doacao.cadastrar(codigos.paraDoacao(), doador, medicamento,
                    lote, validade, fotoUrl,
                    propriedades.doacao().valorMinimoPmc(),
                    propriedades.doacao().diasMinimosDeValidade(),
                    lacreDeclarado)));
        }

        LeituraDaEmbalagem leitura = avaliador.avaliar(fotoUrl);
        criadas.forEach(doacao -> preValidar(doacao, leitura));
        avisarResultadoDaPreValidacao(criadas);
        return criadas;
    }

    /**
     * UC01 passos 5 a 8 (RN10). O resultado da IA e sempre gravado, inclusive
     * quando ela erra: esses registros, comparados com a decisao do
     * farmaceutico, sao o conjunto de dados rotulado da fase 2.
     */
    private void preValidar(Doacao doacao, LeituraDaEmbalagem leitura) {
        AnalisePreValidacao analise = analises.save(new AnalisePreValidacao(doacao, leitura.ean(),
                leitura.lote(), leitura.validade(), leitura.classe(), leitura.certeza(),
                leitura.motivo(),
                leitura.avaliador() != null ? leitura.avaliador() : avaliador.nome()));

        if (regraDaPreValidacao.decidirSobre(analise) == DecisaoDaPreValidacao.SEGUIR) {
            doacao.preValidar(doacao.getDoador());
        } else {
            doacao.enviarParaCentral(regraDaPreValidacao.explicarEncaminhamento(analise), doacao.getDoador());
        }
    }

    private void avisarResultadoDaPreValidacao(List<Doacao> criadas) {
        Doacao primeira = criadas.get(0);
        String nome = primeira.getMedicamento().getNomeComercial();
        String caixas = criadas.size() == 1 ? nome : "%d caixas de %s".formatted(criadas.size(), nome);
        if (primeira.getStatus() == StatusDoacao.PRE_VALIDADA) {
            notificacoes.avisar(primeira.getDoador(), TipoNotificacao.DOACAO_PRE_VALIDADA,
                    "Doação aprovada na pré-validação",
                    "Escolha a farmácia e o horário para entregar %s.".formatted(caixas));
        } else {
            notificacoes.avisar(primeira.getDoador(), TipoNotificacao.DOACAO_EM_ANALISE,
                    "Doação em análise",
                    "Nossa equipe vai conferir a foto de %s. Você recebe a resposta em até 48 horas."
                            .formatted(caixas));
        }
    }

    /** UC10 A2 - o doador envia a foto nova que a central pediu. */
    @Transactional
    public Doacao trocarFoto(String codigo, String novaFotoUrl, Usuario doador) {
        Doacao doacao = buscarPorCodigo(codigo);
        exigirQueSejaODoador(doacao, doador);
        doacao.trocarFoto(novaFotoUrl, doador);
        preValidar(doacao, avaliador.avaliar(novaFotoUrl));
        avisarResultadoDaPreValidacao(List.of(doacao));
        return doacao;
    }

    // --- UC10: central ----------------------------------------------------------

    /** RN10 - a decisao humana sobre o que a IA nao resolveu. */
    @Transactional
    public Doacao decidirNaCentral(String codigo, RevisaoCentral.Decisao decisao,
                                   String justificativa, Usuario analista) {
        exigirPapel(analista, Papel.ADMIN, "revisar doações na central");
        Doacao doacao = buscarPorCodigo(codigo);

        revisoes.save(new RevisaoCentral(doacao, analista, decisao, justificativa));

        switch (decisao) {
            case APROVADA -> {
                doacao.aprovarNaCentral(justificativa, analista);
                notificacoes.avisar(doacao.getDoador(), TipoNotificacao.DOACAO_PRE_VALIDADA,
                        "Doação aprovada", "Escolha a farmácia e o horário para a entrega.");
            }
            case RECUSADA -> {
                doacao.recusar(justificativa, analista);
                notificacoes.avisar(doacao.getDoador(), TipoNotificacao.DOACAO_RECUSADA,
                        "Doação não aprovada", justificativa);
            }
            case NOVA_FOTO -> {
                doacao.pedirNovaFoto(justificativa, analista);
                notificacoes.avisar(doacao.getDoador(), TipoNotificacao.NOVA_FOTO_SOLICITADA,
                        "Envie uma nova foto",
                        "Não conseguimos conferir a foto de %s: %s. Abra a doação no app e tire outra foto."
                                .formatted(doacao.getMedicamento().getNomeComercial(), justificativa));
            }
        }
        return doacao;
    }

    // --- UC02: agendamento ------------------------------------------------------

    /** UC02 passo 1 - horarios com vaga nos proximos dias, em que a farmacia esta aberta. */
    @Transactional(readOnly = true)
    public List<OffsetDateTime> horariosDisponiveis(Long pontoDeColetaId) {
        PontoDeColeta ponto = buscarPontoAtivo(pontoDeColetaId);
        ZonedDateTime agora = ZonedDateTime.now(PontoDeColeta.FUSO);
        ZonedDateTime inicio = agora.plusHours(2).truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1);
        ZonedDateTime fim = agora.toLocalDate().plusDays(DIAS_DE_AGENDA).atStartOfDay(PontoDeColeta.FUSO);

        Map<OffsetDateTime, Long> ocupados = agendamentos
                .ocupadosNoPeriodo(ponto.getId(), inicio.toOffsetDateTime(), fim.toOffsetDateTime())
                .stream()
                .collect(Collectors.groupingBy(
                        a -> a.getDataHora().atZoneSameInstant(PontoDeColeta.FUSO)
                                .truncatedTo(java.time.temporal.ChronoUnit.HOURS).toOffsetDateTime(),
                        Collectors.counting()));

        List<OffsetDateTime> livres = new ArrayList<>();
        for (ZonedDateTime hora = inicio; hora.isBefore(fim); hora = hora.plusHours(1)) {
            OffsetDateTime candidato = hora.toOffsetDateTime();
            if (ponto.funcionaEm(candidato)
                    && ocupados.getOrDefault(candidato, 0L) < ponto.getVagasPorHora()) {
                livres.add(candidato);
            }
        }
        return livres;
    }

    /** UC02 - o doador escolhe onde e quando entregar. */
    @Transactional
    public Agendamento agendar(String codigo, Long pontoDeColetaId,
                               OffsetDateTime dataHora, Usuario doador) {
        Doacao doacao = buscarPorCodigo(codigo);
        exigirQueSejaODoador(doacao, doador);
        // Travado: a contagem de vagas e a gravacao acontecem sem outro
        // agendamento do mesmo ponto no meio (ver PontoDeColetaRepository).
        PontoDeColeta ponto = exigirAtivo(pontos.travarPorId(pontoDeColetaId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Ponto de coleta", pontoDeColetaId)));
        exigirHorarioComVaga(ponto, dataHora);

        if (doacao.getStatus() == StatusDoacao.CANCELADA) {
            // UC02 A2 - reagendar e permitido uma vez so.
            if (agendamentos.countByDoacaoId(doacao.getId()) >= 2) {
                throw new RegraDeNegocioViolada("AGENDAMENTO",
                        "Esta doação já foi reagendada uma vez e não pode ser reagendada de novo");
            }
            exigirValidadeAindaSuficiente(doacao);
            doacao.reagendar(ponto, dataHora, doador);
        } else {
            doacao.agendar(ponto, dataHora, doador);
        }
        return agendamentos.save(new Agendamento(doacao, ponto, dataHora, codigoDeEntregaUnico()));
    }

    @Transactional
    public Doacao cancelarAgendamento(String codigo, String motivo, Usuario solicitante) {
        Doacao doacao = buscarPorCodigo(codigo);
        exigirQueSejaODoador(doacao, solicitante);
        doacao.cancelarAgendamento(motivo, solicitante);
        return doacao;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Agendamento> agendamentoAtualDe(Doacao doacao) {
        return agendamentos.findFirstByDoacaoIdOrderByCriadoEmDesc(doacao.getId());
    }

    /** UC02 A2 - cancelada, reagendamento ainda nao usado e validade ainda suficiente. */
    @Transactional(readOnly = true)
    public boolean podeReagendar(Doacao doacao) {
        return doacao.getStatus() == StatusDoacao.CANCELADA
                && agendamentos.countByDoacaoId(doacao.getId()) < 2
                && doacao.aindaTemValidadeSuficiente(propriedades.doacao().diasMinimosDeValidade());
    }

    // --- UC03: balcao -----------------------------------------------------------

    /** O que chega hoje no ponto de coleta onde o farmaceutico atua. */
    @Transactional(readOnly = true)
    public Page<Doacao> filaDoBalcao(Usuario usuarioFarmaceutico, Pageable pagina) {
        Farmaceutico farmaceutico = buscarFarmaceutico(usuarioFarmaceutico);
        return doacoes.findByPontoDeColetaIdAndStatusInOrderByAtualizadoEmDesc(
                farmaceutico.getPontoDeColeta().getId(), STATUS_DO_BALCAO, pagina);
    }

    private static final List<StatusDoacao> STATUS_DO_BALCAO =
            List.of(StatusDoacao.AGENDADA, StatusDoacao.RECEBIDA, StatusDoacao.VALIDADA);

    /** UC03 passo 1 - o farmaceutico le o codigo de entrega que o doador mostra. */
    @Transactional(readOnly = true)
    public Doacao buscarPorCodigoDeEntrega(String codigoEntrega, Usuario usuarioFarmaceutico) {
        Agendamento agendamento = agendamentos.findFirstByCodigoEntregaOrderByCriadoEmDesc(
                        codigoEntrega.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNaoEncontrado("Código de entrega", codigoEntrega));
        Doacao doacao = agendamento.getDoacao();
        exigirQueAtueNoPontoDaDoacao(buscarFarmaceutico(usuarioFarmaceutico), doacao);
        return doacao;
    }

    /** UC03 A3 - sem o codigo: busca pelo CPF ou telefone do doador. */
    @Transactional(readOnly = true)
    public List<Doacao> buscarPorDocumentoDoDoador(String documento, Usuario usuarioFarmaceutico) {
        Farmaceutico farmaceutico = buscarFarmaceutico(usuarioFarmaceutico);
        String digitos = documento.replaceAll("\\D", "");
        if (digitos.length() < 10) {
            throw new RegraDeNegocioViolada("BUSCA", "Informe o CPF (11 dígitos) ou o telefone com DDD");
        }
        return doacoes.doDoadorNoPonto(farmaceutico.getPontoDeColeta().getId(), STATUS_DO_BALCAO, digitos);
    }

    /** UC03 passo 1 - a caixa chega no balcao. */
    @Transactional
    public Doacao receber(String codigo, Usuario usuarioFarmaceutico) {
        Doacao doacao = buscarPorCodigo(codigo);
        Farmaceutico farmaceutico = buscarFarmaceutico(usuarioFarmaceutico);
        exigirQueAtueNoPontoDaDoacao(farmaceutico, doacao);

        doacao.receber(usuarioFarmaceutico);
        agendamentos.findFirstByDoacaoIdOrderByCriadoEmDesc(doacao.getId())
                .ifPresent(Agendamento::registrarComparecimento);

        notificacoes.avisar(doacao.getDoador(), TipoNotificacao.DOACAO_RECEBIDA,
                "Doação recebida", "A farmácia recebeu %s. Obrigado!"
                        .formatted(doacao.getMedicamento().getNomeComercial()));
        return doacao;
    }

    /**
     * UC03 passos 3 a 6 - RN01: o farmaceutico confere o lacre presencialmente.
     * A1: se lote ou validade reais forem outros, corrige antes de aprovar.
     * Aprovada, a caixa entra no estoque e o matching roda (UC05).
     */
    @Transactional
    public Doacao validar(String codigo, String fotoUrl, String loteCorrigido,
                          LocalDate validadeCorrigida, Usuario usuarioFarmaceutico) {
        Doacao doacao = buscarPorCodigo(codigo);
        Farmaceutico farmaceutico = buscarFarmaceutico(usuarioFarmaceutico);
        exigirQueAtueNoPontoDaDoacao(farmaceutico, doacao);

        doacao.corrigirDados(loteCorrigido, validadeCorrigida,
                propriedades.doacao().diasMinimosDeValidade(), usuarioFarmaceutico);
        exigirValidadeAindaSuficiente(doacao);

        validacoes.save(Validacao.aprovar(doacao, farmaceutico, fotoUrl));
        doacao.validar(usuarioFarmaceutico);
        doacao.disponibilizar(usuarioFarmaceutico);
        ofertas.ofertarDoacao(doacao);
        return doacao;
    }

    public Doacao validar(String codigo, Usuario usuarioFarmaceutico) {
        return validar(codigo, null, null, null, usuarioFarmaceutico);
    }

    /** UC03 A2 - lacre violado ou dados que nao batem. */
    @Transactional
    public Doacao rejeitar(String codigo, boolean lacreIntegro, boolean dadosConferem,
                           String motivo, String fotoUrl, Usuario usuarioFarmaceutico) {
        Doacao doacao = buscarPorCodigo(codigo);
        Farmaceutico farmaceutico = buscarFarmaceutico(usuarioFarmaceutico);
        exigirQueAtueNoPontoDaDoacao(farmaceutico, doacao);

        validacoes.save(Validacao.rejeitar(doacao, farmaceutico, lacreIntegro, dadosConferem, motivo, fotoUrl));
        doacao.rejeitar(motivo, usuarioFarmaceutico);

        notificacoes.avisar(doacao.getDoador(), TipoNotificacao.DOACAO_RECUSADA,
                "Doação não aprovada na conferência", motivo);
        return doacao;
    }

    // --- consultas --------------------------------------------------------------

    @Transactional(readOnly = true)
    public Doacao buscarPorCodigo(String codigo) {
        return doacoes.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNaoEncontrado("Doacao", codigo));
    }

    /**
     * RN05 e RN06 / UC09 - o historico e do doador, da farmacia e da central.
     * Para qualquer outra pessoa a doacao simplesmente "nao existe": dizer
     * "sem permissao" ja confirmaria que aquele codigo e valido.
     */
    @Transactional(readOnly = true)
    public Doacao detalharPara(String codigo, Usuario solicitante) {
        Doacao doacao = buscarPorCodigo(codigo);
        boolean ehDoador = doacao.getDoador().getId().equals(solicitante.getId());
        boolean ehEquipe = solicitante.temPapel(Papel.ADMIN) || solicitante.temPapel(Papel.FARMACEUTICO);
        if (!ehDoador && !ehEquipe) {
            throw new RecursoNaoEncontrado("Doacao", codigo);
        }
        return doacao;
    }

    // --- rotinas (chamadas pela RotinaDeManutencao) ---------------------------

    /** UC02 passo 3 - lembrete para quem entrega amanha. */
    @Transactional
    public int enviarLembretesDaVespera() {
        LocalDate amanha = LocalDate.now(PontoDeColeta.FUSO).plusDays(1);
        List<Agendamento> deAmanha = agendamentos.semLembreteEntre(
                amanha.atStartOfDay(PontoDeColeta.FUSO).toOffsetDateTime(),
                amanha.plusDays(1).atStartOfDay(PontoDeColeta.FUSO).toOffsetDateTime());
        for (Agendamento agendamento : deAmanha) {
            Doacao doacao = agendamento.getDoacao();
            notificacoes.avisar(doacao.getDoador(), TipoNotificacao.LEMBRETE_DE_ENTREGA,
                    "Sua entrega é amanhã",
                    "Leve %s a %s em %s. Mostre o código de entrega %s no balcão."
                            .formatted(doacao.getMedicamento().getNomeComercial(),
                                    agendamento.getPontoDeColeta().getNome(),
                                    DIA_E_HORA.format(agendamento.getDataHora()),
                                    agendamento.getCodigoEntrega()));
            agendamento.marcarLembreteEnviado();
        }
        return deAmanha.size();
    }

    /** UC02 A2 - quem nao compareceu em 7 dias tem o agendamento cancelado (pode reagendar uma vez). */
    @Transactional
    public int cancelarFaltas() {
        List<Agendamento> faltas = agendamentos.faltasAntesDe(OffsetDateTime.now().minus(TOLERANCIA_DE_FALTA));
        for (Agendamento agendamento : faltas) {
            Doacao doacao = agendamento.getDoacao();
            agendamento.registrarFalta();
            doacao.cancelarAgendamento("doador não compareceu em 7 dias", null);
            boolean podeReagendar = agendamentos.countByDoacaoId(doacao.getId()) < 2;
            notificacoes.avisar(doacao.getDoador(), TipoNotificacao.AGENDAMENTO_CANCELADO,
                    "Agendamento cancelado",
                    podeReagendar
                            ? "Não registramos a entrega de %s. Você pode reagendar uma vez pelo app."
                                    .formatted(doacao.getMedicamento().getNomeComercial())
                            : "Não registramos a entrega de %s, e o reagendamento já foi usado."
                                    .formatted(doacao.getMedicamento().getNomeComercial()));
        }
        return faltas.size();
    }

    /**
     * UC10 A3 - avisa a central sobre casos parados ha mais de 48 h. Roda de
     * hora em hora e olha so a faixa da ultima hora, para cada caso gerar um
     * alerta e nao um por rodada.
     */
    @Transactional
    public int alertarCentralSobreAtrasos() {
        OffsetDateTime limite = OffsetDateTime.now().minus(PRAZO_DA_CENTRAL);
        List<Doacao> atrasadas = doacoes.naCentralEntre(limite.minusHours(1), limite);
        if (atrasadas.isEmpty()) {
            return 0;
        }
        String codigosAtrasados = atrasadas.stream().map(Doacao::getCodigo).collect(Collectors.joining(", "));
        for (Usuario admin : usuarios.ativosComPapel(Papel.ADMIN)) {
            notificacoes.avisar(admin, TipoNotificacao.CENTRAL_ATRASADA,
                    "Casos esperando há mais de 48 h",
                    "%d doação(ões) aguardam análise há mais de 48 horas: %s"
                            .formatted(atrasadas.size(), codigosAtrasados));
        }
        return atrasadas.size();
    }

    // --- guardas ----------------------------------------------------------------

    private String codigoDeEntregaUnico() {
        String codigo;
        do {
            codigo = codigos.paraRetirada();
        } while (agendamentos.findFirstByCodigoEntregaOrderByCriadoEmDesc(codigo).isPresent());
        return codigo;
    }

    private PontoDeColeta buscarPontoAtivo(Long pontoDeColetaId) {
        return exigirAtivo(pontos.findById(pontoDeColetaId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Ponto de coleta", pontoDeColetaId)));
    }

    private PontoDeColeta exigirAtivo(PontoDeColeta ponto) {
        if (!ponto.isAtivo()) {
            throw new RegraDeNegocioViolada("PONTO",
                    "%s não está recebendo doações no momento".formatted(ponto.getNome()));
        }
        return ponto;
    }

    private void exigirHorarioComVaga(PontoDeColeta ponto, OffsetDateTime dataHora) {
        if (dataHora.isBefore(OffsetDateTime.now())) {
            throw new RegraDeNegocioViolada("AGENDAMENTO", "Escolha uma data e hora futuras para a entrega");
        }
        if (!ponto.funcionaEm(dataHora)) {
            throw new RegraDeNegocioViolada("AGENDAMENTO",
                    "%s não funciona neste horário. Horário: %s".formatted(ponto.getNome(), ponto.getHorario()));
        }
        OffsetDateTime inicioDaHora = dataHora.atZoneSameInstant(PontoDeColeta.FUSO)
                .truncatedTo(java.time.temporal.ChronoUnit.HOURS).toOffsetDateTime();
        long ocupados = agendamentos.ocupadosNoPeriodo(ponto.getId(), inicioDaHora, inicioDaHora.plusHours(1)).size();
        if (ocupados >= ponto.getVagasPorHora()) {
            throw new RegraDeNegocioViolada("AGENDAMENTO",
                    "Este horário já está cheio em %s. Escolha outro.".formatted(ponto.getNome()));
        }
    }

    private void exigirQueSejaODoador(Doacao doacao, Usuario usuario) {
        if (!doacao.getDoador().getId().equals(usuario.getId())) {
            throw new RegraDeNegocioViolada("RN05", "Esta doação pertence a outra pessoa");
        }
    }

    private void exigirValidadeAindaSuficiente(Doacao doacao) {
        int diasMinimos = propriedades.doacao().diasMinimosDeValidade();
        if (!doacao.aindaTemValidadeSuficiente(diasMinimos)) {
            throw new RegraDeNegocioViolada("RN02",
                    "A caixa vence em %d dia(s) e não alcança os %d dias mínimos"
                            .formatted(doacao.diasAteVencer(), diasMinimos));
        }
    }

    private Farmaceutico buscarFarmaceutico(Usuario usuario) {
        return farmaceuticos.findById(usuario.getId())
                .orElseThrow(() -> new RegraDeNegocioViolada("RN10",
                        "Somente um farmacêutico cadastrado pode conferir doações"));
    }

    private void exigirQueAtueNoPontoDaDoacao(Farmaceutico farmaceutico, Doacao doacao) {
        if (doacao.getPontoDeColeta() == null
                || !farmaceutico.atuaEm(doacao.getPontoDeColeta())) {
            throw new RegraDeNegocioViolada("RN10",
                    "Esta doação foi agendada para outro ponto de coleta");
        }
    }

    private void exigirPapel(Usuario usuario, Papel papel, String operacao) {
        if (!usuario.temPapel(papel)) {
            throw new RegraDeNegocioViolada("PERMISSAO",
                    "Seu perfil não permite %s".formatted(operacao));
        }
    }
}
