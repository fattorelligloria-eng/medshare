package br.com.medshare.doacao;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.farmacia.PontoDeColeta;
import br.com.medshare.medicamento.Medicamento;
import br.com.medshare.usuario.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Uma caixa doada, rastreada do cadastro ate a entrega.
 *
 * As regras de transicao moram aqui dentro, e nao no service. O motivo e
 * simples: se a regra estivesse no service, bastaria um caminho novo no codigo
 * (um teste, um script, um segundo service) para que uma doacao pulasse de
 * CADASTRADA para ENTREGUE sem passar pela farmacia. Com a regra dentro do
 * objeto, isso nao e possivel por construcao.
 *
 * Todo metodo de transicao grava um evento no historico (RN06). Nao existe
 * caminho que mude o status sem deixar rastro.
 */
@Entity
@Table(name = "doacao")
public class Doacao {

    /**
     * O historico e lido por gente, entao data vai no formato e no fuso daqui.
     * O fuso e explicito porque o horario chega em UTC do app e o servidor pode
     * rodar em UTC: sem ele, "14:00" virava "17:00" num registro imutavel.
     */
    private static final DateTimeFormatter FORMATO_BRASILEIRO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
                    .withZone(ZoneId.of("America/Sao_Paulo"));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String codigo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doador_id", nullable = false)
    private Usuario doador;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ponto_coleta_id")
    private PontoDeColeta pontoDeColeta;

    @Column(nullable = false)
    private String lote;

    @Column(nullable = false)
    private LocalDate validade;

    @Column(name = "foto_url", nullable = false)
    private String fotoUrl;

    /** RN01 - o doador declarou, no cadastro, que a embalagem esta lacrada. */
    @Column(name = "lacre_declarado", nullable = false)
    private boolean lacreDeclarado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDoacao status = StatusDoacao.CADASTRADA;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    /**
     * Composicao: o historico so existe enquanto a doacao existe.
     * Sem orphanRemoval de proposito — evento nenhum e removido (RN06).
     */
    @OneToMany(mappedBy = "doacao", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("ocorridoEm ASC, id ASC")
    private List<EventoHistorico> historico = new ArrayList<>();

    protected Doacao() { }

    private Doacao(String codigo, Usuario doador, Medicamento medicamento,
                   String lote, LocalDate validade, String fotoUrl) {
        this.codigo = codigo;
        this.doador = doador;
        this.medicamento = medicamento;
        this.lote = lote;
        this.validade = validade;
        this.fotoUrl = fotoUrl;
    }

    /**
     * Unica porta de entrada para criar uma doacao. As regras que decidem se a
     * caixa pode sequer ser cadastrada sao conferidas aqui, antes de o objeto
     * existir — nao ha como construir uma doacao invalida.
     *
     * @param pisoDePreco       RN07 - PMC minimo, vindo da configuracao
     * @param diasMinimos       RN02 - validade minima, vinda da configuracao
     */
    public static Doacao cadastrar(String codigo, Usuario doador, Medicamento medicamento,
                                   String lote, LocalDate validade, String fotoUrl,
                                   BigDecimal pisoDePreco, int diasMinimos) {
        return cadastrar(codigo, doador, medicamento, lote, validade, fotoUrl,
                pisoDePreco, diasMinimos, true);
    }

    /**
     * @param lacreDeclarado RN01 - o doador confirma que a embalagem esta
     *                       lacrada. Sem a declaracao, a doacao nem e criada.
     */
    public static Doacao cadastrar(String codigo, Usuario doador, Medicamento medicamento,
                                   String lote, LocalDate validade, String fotoUrl,
                                   BigDecimal pisoDePreco, int diasMinimos,
                                   boolean lacreDeclarado) {
        if (!lacreDeclarado) {
            throw new RegraDeNegocioViolada("RN01",
                    "Só aceitamos embalagem lacrada. Confirme que a caixa nunca foi aberta.");
        }
        exigirMedicamentoDeAltoCusto(medicamento, pisoDePreco);
        exigirValidadeSuficiente(validade, diasMinimos, medicamento);

        Doacao doacao = new Doacao(codigo, doador, medicamento, lote, validade, fotoUrl);
        doacao.lacreDeclarado = true;
        doacao.registrarEvento(TipoEvento.CADASTRO,
                "Doação cadastrada pelo doador", null, StatusDoacao.CADASTRADA, doador);
        return doacao;
    }

    private static void exigirMedicamentoDeAltoCusto(Medicamento medicamento, BigDecimal piso) {
        if (!medicamento.atendeAoPisoDePreco(piso)) {
            throw new RegraDeNegocioViolada("RN07",
                    "A rede só aceita medicamentos com PMC igual ou acima de R$ %s. %s custa R$ %s."
                            .formatted(piso, medicamento.getNomeComercial(), medicamento.getPmc()));
        }
    }

    private static void exigirValidadeSuficiente(LocalDate validade, int diasMinimos,
                                                 Medicamento medicamento) {
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), validade);
        if (diasRestantes < diasMinimos) {
            throw new RegraDeNegocioViolada("RN02",
                    "%s vence em %d dia(s). São necessários no mínimo %d dias de validade."
                            .formatted(medicamento.getNomeComercial(), diasRestantes, diasMinimos));
        }
    }

    // --- transicoes do ciclo de vida -----------------------------------------

    /** RN10 - a leitura automatica bateu com o cadastro. */
    public void preValidar(Usuario responsavel) {
        mudarPara(StatusDoacao.PRE_VALIDADA, TipoEvento.PRE_VALIDACAO,
                "Leitura da embalagem conferiu com o cadastro", responsavel);
    }

    /** RN10 - divergencia ou duvida: quem decide e uma pessoa, nao a IA. */
    public void enviarParaCentral(String motivo, Usuario responsavel) {
        mudarPara(StatusDoacao.EM_ANALISE_CENTRAL, TipoEvento.ENVIO_PARA_CENTRAL,
                "Enviada para análise humana: " + motivo, responsavel);
    }

    public void aprovarNaCentral(String justificativa, Usuario analista) {
        mudarPara(StatusDoacao.PRE_VALIDADA, TipoEvento.DECISAO_DA_CENTRAL,
                "Central aprovou: " + justificativa, analista);
    }

    public void recusar(String motivo, Usuario responsavel) {
        mudarPara(StatusDoacao.RECUSADA, TipoEvento.RECUSA,
                "Doação recusada: " + motivo, responsavel);
    }

    public void agendar(PontoDeColeta ponto, OffsetDateTime quando, Usuario responsavel) {
        mudarPara(StatusDoacao.AGENDADA, TipoEvento.AGENDAMENTO,
                "Entrega agendada em %s para %s"
                        .formatted(ponto.getNome(), FORMATO_BRASILEIRO.format(quando)),
                responsavel);
        this.pontoDeColeta = ponto;
    }

    public void cancelarAgendamento(String motivo, Usuario responsavel) {
        mudarPara(StatusDoacao.CANCELADA, TipoEvento.CANCELAMENTO,
                "Agendamento cancelado: " + motivo, responsavel);
    }

    /** UC02 A2 - nova data depois de um cancelamento. O limite de uma vez fica no service. */
    public void reagendar(PontoDeColeta ponto, OffsetDateTime quando, Usuario responsavel) {
        mudarPara(StatusDoacao.AGENDADA, TipoEvento.REAGENDAMENTO,
                "Entrega reagendada em %s para %s"
                        .formatted(ponto.getNome(), FORMATO_BRASILEIRO.format(quando)),
                responsavel);
        this.pontoDeColeta = ponto;
    }

    /** UC10 A2 - a central nao conseguiu decidir pela foto e pediu outra ao doador. */
    public void pedirNovaFoto(String motivo, Usuario analista) {
        mudarPara(StatusDoacao.CADASTRADA, TipoEvento.NOVA_FOTO_SOLICITADA,
                "Central pediu uma nova foto: " + motivo, analista);
    }

    /** UC10 A2 - o doador enviou a foto nova; a pre-validacao roda de novo. */
    public void trocarFoto(String novaFotoUrl, Usuario doador) {
        if (status != StatusDoacao.CADASTRADA) {
            throw new RegraDeNegocioViolada("CICLO",
                    "A doação %s está em %s; só dá para trocar a foto quando a central pedir."
                            .formatted(codigo, status));
        }
        this.fotoUrl = novaFotoUrl;
        this.atualizadoEm = OffsetDateTime.now();
        registrarEvento(TipoEvento.NOVA_FOTO_ENVIADA, "Doador enviou uma nova foto da embalagem",
                status, status, doador);
    }

    /**
     * UC03 A1 - no balcao, o farmaceutico ve que lote ou validade reais sao
     * outros e corrige. O valor antigo fica no historico (RN06), e a validade
     * corrigida ainda precisa passar na RN02.
     */
    public void corrigirDados(String novoLote, LocalDate novaValidade, int diasMinimos,
                              Usuario farmaceutico) {
        if (status != StatusDoacao.RECEBIDA) {
            throw new RegraDeNegocioViolada("CICLO",
                    "Só dá para corrigir lote e validade com a caixa no balcão (status RECEBIDA)");
        }
        List<String> mudancas = new ArrayList<>();
        if (novoLote != null && !novoLote.isBlank() && !novoLote.equals(lote)) {
            mudancas.add("lote %s → %s".formatted(lote, novoLote));
            this.lote = novoLote;
        }
        if (novaValidade != null && !novaValidade.equals(validade)) {
            exigirValidadeSuficiente(novaValidade, diasMinimos, medicamento);
            mudancas.add("validade %s → %s".formatted(validade, novaValidade));
            this.validade = novaValidade;
        }
        if (mudancas.isEmpty()) {
            return;
        }
        this.atualizadoEm = OffsetDateTime.now();
        registrarEvento(TipoEvento.CORRECAO_DE_DADOS,
                "Farmacêutico corrigiu " + String.join(" e ", mudancas), status, status, farmaceutico);
    }

    /**
     * UC08 A2 - antes de desativar uma farmacia, o estoque vai para outra.
     * So caixa DISPONIVEL se move: reservada ja tem beneficiario a caminho.
     */
    public void transferirPara(PontoDeColeta destino, Usuario responsavel) {
        if (status != StatusDoacao.DISPONIVEL) {
            throw new RegraDeNegocioViolada("CICLO",
                    "Só caixas disponíveis podem ser transferidas; %s está em %s".formatted(codigo, status));
        }
        String origem = pontoDeColeta == null ? "—" : pontoDeColeta.getNome();
        this.pontoDeColeta = destino;
        this.atualizadoEm = OffsetDateTime.now();
        registrarEvento(TipoEvento.TRANSFERENCIA,
                "Transferida de %s para %s".formatted(origem, destino.getNome()),
                status, status, responsavel);
    }

    public void receber(Usuario farmaceutico) {
        mudarPara(StatusDoacao.RECEBIDA, TipoEvento.RECEBIMENTO,
                "Caixa recebida no balcão", farmaceutico);
    }

    /** RN01 - o farmaceutico conferiu o lacre presencialmente e aprovou. */
    public void validar(Usuario farmaceutico) {
        mudarPara(StatusDoacao.VALIDADA, TipoEvento.VALIDACAO,
                "Lacre e dados conferidos pelo farmacêutico", farmaceutico);
    }

    public void rejeitar(String motivo, Usuario farmaceutico) {
        mudarPara(StatusDoacao.REJEITADA, TipoEvento.REJEICAO,
                "Reprovada na conferência: " + motivo, farmaceutico);
    }

    public void disponibilizar(Usuario responsavel) {
        mudarPara(StatusDoacao.DISPONIVEL, TipoEvento.DISPONIBILIZACAO,
                "Disponível para beneficiários", responsavel);
    }

    /**
     * RN05 - o codigo de retirada NAO entra no historico: o doador le este
     * historico, e com o codigo em maos poderia se passar pelo beneficiario.
     */
    public void reservar(Usuario responsavel) {
        mudarPara(StatusDoacao.RESERVADA, TipoEvento.RESERVA,
                "Reservada por um beneficiário", responsavel);
    }

    public void liberarReservaExpirada() {
        mudarPara(StatusDoacao.DISPONIVEL, TipoEvento.EXPIRACAO_DE_RESERVA,
                "Reserva expirou sem retirada; voltou para o estoque", null);
    }

    public void liberarReservaCancelada(Usuario beneficiario) {
        liberarReservaCancelada("Reserva cancelada pelo beneficiário", beneficiario);
    }

    /** Reserva desfeita (pelo beneficiario ou por entrega negada no balcao). */
    public void liberarReservaCancelada(String motivo, Usuario responsavel) {
        mudarPara(StatusDoacao.DISPONIVEL, TipoEvento.CANCELAMENTO_DE_RESERVA,
                motivo + "; voltou para o estoque", responsavel);
    }

    /** UC05 - a caixa foi oferecida a alguem da fila (sem nome: RN05). */
    public void registrarOferta(Usuario sistema) {
        registrarEvento(TipoEvento.OFERTA, "Oferecida a um beneficiário da fila", status, status, sistema);
    }

    public void entregar(Usuario farmaceutico) {
        mudarPara(StatusDoacao.ENTREGUE, TipoEvento.ENTREGA,
                "Entregue ao beneficiário mediante receita e documento", farmaceutico);
    }

    public void descartar(String motivo, Usuario responsavel) {
        mudarPara(StatusDoacao.DESCARTADA, TipoEvento.DESCARTE,
                "Descartada: " + motivo, responsavel);
    }

    // --- consultas de dominio -------------------------------------------------

    /** RN02 - a validade minima vale tambem enquanto a caixa espera na prateleira. */
    public boolean aindaTemValidadeSuficiente(int diasMinimos) {
        return ChronoUnit.DAYS.between(LocalDate.now(), validade) >= diasMinimos;
    }

    public long diasAteVencer() {
        return ChronoUnit.DAYS.between(LocalDate.now(), validade);
    }

    public boolean estaDisponivel() {
        return status == StatusDoacao.DISPONIVEL;
    }

    // --- mecanica da transicao ------------------------------------------------

    private void mudarPara(StatusDoacao novoStatus, TipoEvento tipo,
                           String descricao, Usuario responsavel) {
        if (!status.podeIrPara(novoStatus)) {
            throw new RegraDeNegocioViolada("CICLO",
                    "A doação %s está em %s e não pode ir para %s. Transições possíveis: %s."
                            .formatted(codigo, status, novoStatus, status.proximosPossiveis()));
        }
        StatusDoacao anterior = status;
        status = novoStatus;
        atualizadoEm = OffsetDateTime.now();
        registrarEvento(tipo, descricao, anterior, novoStatus, responsavel);
    }

    private void registrarEvento(TipoEvento tipo, String descricao,
                                 StatusDoacao anterior, StatusDoacao novo, Usuario responsavel) {
        historico.add(new EventoHistorico(this, tipo, descricao, anterior, novo, responsavel));
    }

    // --- acessores ------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Usuario getDoador() {
        return doador;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public PontoDeColeta getPontoDeColeta() {
        return pontoDeColeta;
    }

    public String getLote() {
        return lote;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public boolean isLacreDeclarado() {
        return lacreDeclarado;
    }

    public StatusDoacao getStatus() {
        return status;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    /** Lista de leitura: quem quiser um evento novo usa uma transicao. */
    public List<EventoHistorico> getHistorico() {
        return List.copyOf(historico);
    }
}
