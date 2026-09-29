package br.com.medshare.necessidade;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.medicamento.Medicamento;
import br.com.medshare.usuario.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * O pedido do beneficiario: "preciso deste medicamento".
 *
 * RN04 - so pode existir uma necessidade ativa por medicamento por pessoa.
 * A regra e garantida por indice unico parcial no banco
 * (uq_necessidade_ativa), entao nem uma corrida entre duas requisicoes
 * simultaneas consegue criar duas.
 */
@Entity
@Table(name = "necessidade")
public class Necessidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "beneficiario_id", nullable = false)
    private Usuario beneficiario;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private OffsetDateTime criadaEm = OffsetDateTime.now();

    @Column(name = "encerrada_em")
    private OffsetDateTime encerradaEm;

    @OneToOne(mappedBy = "necessidade", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Receita receita;

    /** UC07 A2 - perdeu a caixa porque a validade caiu: volta a fila na frente. */
    @Column(nullable = false)
    private boolean prioridade;

    /** UC07 A1 - a receita nao bateu no balcao; sai da fila ate mandar outra. */
    @Column(name = "em_revisao", nullable = false)
    private boolean emRevisao;

    @Column(name = "motivo_revisao")
    private String motivoRevisao;

    protected Necessidade() { }

    public Necessidade(Usuario beneficiario, Medicamento medicamento) {
        this.beneficiario = beneficiario;
        this.medicamento = medicamento;
    }

    public void anexarReceita(Receita receita) {
        this.receita = receita;
        receitaRenovada();
    }

    /** Receita nova (ou trocada) tira o pedido da revisao. */
    public void receitaRenovada() {
        this.emRevisao = false;
        this.motivoRevisao = null;
    }

    /**
     * UC04 A4 - o pedido do mesmo principio ativo e atualizado em vez de
     * duplicado (ex.: a pessoa escolheu outra apresentacao do mesmo remedio).
     */
    public void trocarMedicamento(Medicamento novo) {
        this.medicamento = novo;
    }

    public void priorizar() {
        this.prioridade = true;
    }

    public void marcarParaRevisao(String motivo) {
        this.emRevisao = true;
        this.motivoRevisao = motivo;
    }

    /**
     * Pode receber oferta agora? Ativo, fora de revisao e com receita valida
     * (RN03). O CadUnico (RN08) e conferido a parte, porque e do beneficiario.
     */
    public boolean aptaParaOferta() {
        return ativa && !emRevisao && receita != null && receita.estaValida();
    }

    public boolean isPrioridade() {
        return prioridade;
    }

    public boolean isEmRevisao() {
        return emRevisao;
    }

    public String getMotivoRevisao() {
        return motivoRevisao;
    }

    public void encerrar() {
        this.ativa = false;
        this.encerradaEm = OffsetDateTime.now();
    }

    /**
     * RN03 - a retirada so acontece com receita valida para o principio ativo.
     * Conferido antes de reservar para nao prender uma caixa para alguem que
     * nao vai conseguir retira-la.
     */
    public void exigirReceitaValida() {
        if (receita == null) {
            throw new RegraDeNegocioViolada("RN03",
                    "Envie a receita médica antes de reservar %s"
                            .formatted(medicamento.getNomeComercial()));
        }
        if (!receita.estaValida()) {
            throw new RegraDeNegocioViolada("RN03",
                    "A receita anexada venceu em %s. Envie uma receita dentro da validade."
                            .formatted(receita.getValidade()));
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getBeneficiario() {
        return beneficiario;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public OffsetDateTime getCriadaEm() {
        return criadaEm;
    }

    public OffsetDateTime getEncerradaEm() {
        return encerradaEm;
    }

    public Receita getReceita() {
        return receita;
    }
}
