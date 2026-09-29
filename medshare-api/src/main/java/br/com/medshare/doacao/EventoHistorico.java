package br.com.medshare.doacao;

import br.com.medshare.usuario.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * RN06 - historico completo e imutavel da unidade doada.
 *
 * Nao existe setter nesta classe, e o banco bloqueia UPDATE e DELETE por
 * gatilho. Uma vez gravado, o evento fica. E o que permite responder "por onde
 * essa caixa passou" meses depois, com confianca.
 */
@Entity
@Table(name = "evento_historico")
public class EventoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doacao_id", nullable = false, updatable = false)
    private Doacao doacao;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private OffsetDateTime ocorridoEm = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoEvento tipo;

    @Column(nullable = false, updatable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", updatable = false)
    private StatusDoacao statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", updatable = false)
    private StatusDoacao statusNovo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id", updatable = false)
    private Usuario responsavel;

    protected EventoHistorico() { }

    EventoHistorico(Doacao doacao, TipoEvento tipo, String descricao,
                    StatusDoacao statusAnterior, StatusDoacao statusNovo, Usuario responsavel) {
        this.doacao = doacao;
        this.tipo = tipo;
        this.descricao = descricao;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.responsavel = responsavel;
    }

    public Long getId() {
        return id;
    }

    public Doacao getDoacao() {
        return doacao;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public StatusDoacao getStatusAnterior() {
        return statusAnterior;
    }

    public StatusDoacao getStatusNovo() {
        return statusNovo;
    }

    public Usuario getResponsavel() {
        return responsavel;
    }
}
