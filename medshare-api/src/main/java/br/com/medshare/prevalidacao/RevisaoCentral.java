package br.com.medshare.prevalidacao;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.usuario.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/** RN10 - o registro da decisao humana sobre um caso encaminhado pela IA. */
@Entity
@Table(name = "revisao_central")
public class RevisaoCentral {

    public enum Decisao {
        APROVADA,
        RECUSADA,
        /** UC10 A2 - nao da para decidir pela foto; o doador envia outra. */
        NOVA_FOTO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doacao_id", nullable = false)
    private Doacao doacao;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "analista_id", nullable = false)
    private Usuario analista;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Decisao decisao;

    @Column(nullable = false)
    private String justificativa;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected RevisaoCentral() { }

    public RevisaoCentral(Doacao doacao, Usuario analista, Decisao decisao, String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraDeNegocioViolada("RN10",
                    "Toda decisão da central precisa de justificativa registrada");
        }
        this.doacao = doacao;
        this.analista = analista;
        this.decisao = decisao;
        this.justificativa = justificativa;
    }

    public Long getId() {
        return id;
    }

    public Doacao getDoacao() {
        return doacao;
    }

    public Usuario getAnalista() {
        return analista;
    }

    public Decisao getDecisao() {
        return decisao;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
