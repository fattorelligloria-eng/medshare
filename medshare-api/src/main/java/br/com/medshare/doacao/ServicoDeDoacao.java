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
import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * O caminho da doacao, do cadastro ate ficar disponivel na prateleira.
 *
 * O service coordena — busca, salva, avisa. Quem sabe se uma transicao e
 * permitida e a propria Doacao; quem sabe se a IA pode decidir sozinha e a
 * RegraDeDecisaoDaPreValidacao. Aqui nao ha "if" de regra de negocio, e isso e
 * proposital: regra espalhada por service e regra que um dia sera esquecida em
 * um caminho novo.
 */
@Service
public class ServicoDeDoacao {

    private final DoacaoRepository doacoes;
    private final MedicamentoRepository medicamentos;
    private final PontoDeColetaRepository pontos;
    private final FarmaceuticoRepository farmaceuticos;
    private final AgendamentoRepository agendamentos;
    private final ValidacaoRepository validacoes;
    private final AnalisePreValidacaoRepository analises;
    private final RevisaoCentralRepository revisoes;

    private final AvaliadorDeEmbalagem avaliador;
    private final RegraDeDecisaoDaPreValidacao regraDaPreValidacao;
    private final GeradorDeCodigo codigos;
    private final ServicoDeNotificacao notificacoes;
    private final PropriedadesDoMedShare propriedades;

    public ServicoDeDoacao(DoacaoRepository doacoes, MedicamentoRepository medicamentos,
                           PontoDeColetaRepository pontos, FarmaceuticoRepository farmaceuticos,
                           AgendamentoRepository agendamentos, ValidacaoRepository validacoes,
                           AnalisePreValidacaoRepository analises, RevisaoCentralRepository revisoes,
                           AvaliadorDeEmbalagem avaliador,
                           RegraDeDecisaoDaPreValidacao regraDaPreValidacao,
                           GeradorDeCodigo codigos, ServicoDeNotificacao notificacoes,
                           PropriedadesDoMedShare propriedades) {
        this.doacoes = doacoes;
        this.medicamentos = medicamentos;
        this.pontos = pontos;
        this.farmaceuticos = farmaceuticos;
        this.agendamentos = agendamentos;
        this.validacoes = validacoes;
        this.analises = analises;
        this.revisoes = revisoes;
        this.avaliador = avaliador;
        this.regraDaPreValidacao = regraDaPreValidacao;
        this.codigos = codigos;
        this.notificacoes = notificacoes;
        this.propriedades = propriedades;
    }

    /**
     * Passo 1 - o doador cadastra a caixa pelo aplicativo.
     * A propria criacao ja aplica RN07 (PMC minimo) e RN02 (validade minima):
     * se alguma falhar, nenhuma linha e gravada.
     */
    @Transactional
    public Doacao cadastrar(Long medicamentoId, String lote, java.time.LocalDate validade,
                            String fotoUrl, Usuario doador) {
        Medicamento medicamento = medicamentos.findById(medicamentoId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Medicamento", medicamentoId));

        Doacao doacao = Doacao.cadastrar(codigos.paraDoacao(), doador, medicamento, lote,
                validade, fotoUrl,
                propriedades.doacao().valorMinimoPmc(),
                propriedades.doacao().diasMinimosDeValidade());

        doacoes.save(doacao);
        preValidar(doacao);
        return doacao;
    }

    /**
     * Passo 2 - leitura automatica da foto (RN10).
     * O resultado da IA e sempre gravado, inclusive quando ela erra: esses
     * registros, comparados com a decisao do farmaceutico la na frente, sao o
     * conjunto de dados rotulado da fase 2.
     */
    private void preValidar(Doacao doacao) {
        LeituraDaEmbalagem leitura = avaliador.avaliar(doacao.getFotoUrl());

        AnalisePreValidacao analise = new AnalisePreValidacao(doacao, leitura.ean(),
                leitura.lote(), leitura.validade(), leitura.classe(), leitura.certeza(),
                leitura.motivo(), avaliador.nome());
        analises.save(analise);

        if (regraDaPreValidacao.decidirSobre(analise) == DecisaoDaPreValidacao.SEGUIR) {
            doacao.preValidar(doacao.getDoador());
            notificacoes.avisar(doacao.getDoador(), TipoNotificacao.DOACAO_PRE_VALIDADA,
                    "Doação aprovada na pré-validação",
                    "Escolha a farmácia e o horário para entregar %s."
                            .formatted(doacao.getMedicamento().getNomeComercial()));
            return;
        }

        String motivo = regraDaPreValidacao.explicarEncaminhamento(analise);
        doacao.enviarParaCentral(motivo, doacao.getDoador());
        notificacoes.avisar(doacao.getDoador(), TipoNotificacao.DOACAO_EM_ANALISE,
                "Doação em análise",
                "Nossa equipe vai conferir a foto de %s. Você recebe a resposta em até 48 horas."
                        .formatted(doacao.getMedicamento().getNomeComercial()));
    }

    /** Passo 2b - RN10: a decisao humana sobre o que a IA nao resolveu. */
    @Transactional
    public Doacao decidirNaCentral(String codigo, RevisaoCentral.Decisao decisao,
                                   String justificativa, Usuario analista) {
        exigirPapel(analista, Papel.ADMIN, "revisar doacoes na central");
        Doacao doacao = buscarPorCodigo(codigo);

        revisoes.save(new RevisaoCentral(doacao, analista, decisao, justificativa));

        if (decisao == RevisaoCentral.Decisao.APROVADA) {
            doacao.aprovarNaCentral(justificativa, analista);
            notificacoes.avisar(doacao.getDoador(), TipoNotificacao.DOACAO_PRE_VALIDADA,
                    "Doação aprovada", "Escolha a farmácia e o horário para a entrega.");
        } else {
            doacao.recusar(justificativa, analista);
            notificacoes.avisar(doacao.getDoador(), TipoNotificacao.DOACAO_RECUSADA,
                    "Doação não aprovada", justificativa);
        }
        return doacao;
    }

    /** Passo 3 - o doador escolhe onde e quando entregar. */
    @Transactional
    public Agendamento agendar(String codigo, Long pontoDeColetaId,
                               OffsetDateTime dataHora, Usuario doador) {
        Doacao doacao = buscarPorCodigo(codigo);
        exigirQueSejaODoador(doacao, doador);
        exigirHorarioFuturo(dataHora);

        PontoDeColeta ponto = pontos.findById(pontoDeColetaId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Ponto de coleta", pontoDeColetaId));
        if (!ponto.isAtivo()) {
            throw new RegraDeNegocioViolada("PONTO",
                    "%s não está recebendo doações no momento".formatted(ponto.getNome()));
        }

        doacao.agendar(ponto, dataHora, doador);
        return agendamentos.save(
                new Agendamento(doacao, ponto, dataHora, codigos.paraRetirada()));
    }

    @Transactional
    public Doacao cancelarAgendamento(String codigo, String motivo, Usuario solicitante) {
        Doacao doacao = buscarPorCodigo(codigo);
        exigirQueSejaODoador(doacao, solicitante);
        doacao.cancelarAgendamento(motivo, solicitante);
        return doacao;
    }

    /** Passo 4 - a caixa chega no balcao. */
    @Transactional
    public Doacao receber(String codigo, Usuario usuarioFarmaceutico) {
        Doacao doacao = buscarPorCodigo(codigo);
        Farmaceutico farmaceutico = buscarFarmaceutico(usuarioFarmaceutico);
        exigirQueAtueNoPontoDaDoacao(farmaceutico, doacao);

        doacao.receber(usuarioFarmaceutico);
        agendamentos.findByDoacaoId(doacao.getId()).ifPresent(Agendamento::registrarComparecimento);

        notificacoes.avisar(doacao.getDoador(), TipoNotificacao.DOACAO_RECEBIDA,
                "Doação recebida", "A farmácia recebeu %s. Obrigado!"
                        .formatted(doacao.getMedicamento().getNomeComercial()));
        return doacao;
    }

    /**
     * Passo 5 - RN01: o farmaceutico confere o lacre presencialmente.
     * Aprovada a conferencia, a caixa ja entra no estoque — nao faz sentido um
     * passo manual a mais entre "esta boa" e "esta disponivel".
     */
    @Transactional
    public Doacao validar(String codigo, Usuario usuarioFarmaceutico) {
        Doacao doacao = buscarPorCodigo(codigo);
        Farmaceutico farmaceutico = buscarFarmaceutico(usuarioFarmaceutico);
        exigirQueAtueNoPontoDaDoacao(farmaceutico, doacao);
        exigirValidadeAindaSuficiente(doacao);

        validacoes.save(Validacao.aprovar(doacao, farmaceutico));
        doacao.validar(usuarioFarmaceutico);
        doacao.disponibilizar(usuarioFarmaceutico);
        return doacao;
    }

    /** Passo 5b - RN01: lacre violado ou dados que nao batem. */
    @Transactional
    public Doacao rejeitar(String codigo, boolean lacreIntegro, boolean dadosConferem,
                           String motivo, Usuario usuarioFarmaceutico) {
        Doacao doacao = buscarPorCodigo(codigo);
        Farmaceutico farmaceutico = buscarFarmaceutico(usuarioFarmaceutico);
        exigirQueAtueNoPontoDaDoacao(farmaceutico, doacao);

        validacoes.save(Validacao.rejeitar(doacao, farmaceutico, lacreIntegro, dadosConferem, motivo));
        doacao.rejeitar(motivo, usuarioFarmaceutico);

        notificacoes.avisar(doacao.getDoador(), TipoNotificacao.DOACAO_RECUSADA,
                "Doação não aprovada na conferência", motivo);
        return doacao;
    }

    @Transactional(readOnly = true)
    public Doacao buscarPorCodigo(String codigo) {
        return doacoes.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNaoEncontrado("Doacao", codigo));
    }

    // --- guardas -------------------------------------------------------------

    private void exigirQueSejaODoador(Doacao doacao, Usuario usuario) {
        if (!doacao.getDoador().getId().equals(usuario.getId())) {
            throw new RegraDeNegocioViolada("RN05",
                    "Esta doação pertence a outra pessoa");
        }
    }

    private void exigirHorarioFuturo(OffsetDateTime dataHora) {
        if (dataHora.isBefore(OffsetDateTime.now())) {
            throw new RegraDeNegocioViolada("AGENDAMENTO",
                    "Escolha uma data e hora futuras para a entrega");
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
