package br.com.medshare.reserva;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.necessidade.Necessidade;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * UC05/UC06 - uma caixa disponivel oferecida a uma pessoa da fila.
 *
 * A pessoa tem um prazo (24 h) para aceitar. Aceitou: vira Reserva.
 * Recusou ou deixou vencer: a oferta passa para a proxima da fila. Enquanto a
 * oferta esta PENDENTE, a caixa nao e oferecida a mais ninguem — indice unico
 * parcial no banco garante isso.
 */
@Entity
@Table(name = "oferta")
public class Oferta {

    public enum Status { PENDENTE, ACEITA, RECUSADA, EXPIRADA, CANCELADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "doacao_id", nullable = false)
    private Doacao doacao;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "necessidade_id", nullable = false)
    private Necessidade necessidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDENTE;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private OffsetDateTime criadaEm = OffsetDateTime.now();

    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    @Column(name = "respondida_em")
    private OffsetDateTime respondidaEm;

    protected Oferta() { }

    public Oferta(Doacao doacao, Necessidade necessidade, Duration prazo) {
        this.doacao = doacao;
        this.necessidade = necessidade;
        this.expiraEm = criadaEm.plus(prazo);
    }

    public boolean venceu() {
        return status == Status.PENDENTE && OffsetDateTime.now().isAfter(expiraEm);
    }

    public void aceitar() {
        exigirPendente("aceitar");
        if (venceu()) {
            throw new RegraDeNegocioViolada("OFERTA",
                    "O prazo para aceitar esta oferta terminou. Você continua na fila.");
        }
        encerrar(Status.ACEITA);
    }

    public void recusar() {
        exigirPendente("recusar");
        encerrar(Status.RECUSADA);
    }

    public void expirar() {
        exigirPendente("expirar");
        encerrar(Status.EXPIRADA);
    }

    /** A caixa deixou de estar disponivel (descartada, transferida, etc.). */
    public void cancelar() {
        exigirPendente("cancelar");
        encerrar(Status.CANCELADA);
    }

    private void encerrar(Status final_) {
        this.status = final_;
        this.respondidaEm = OffsetDateTime.now();
    }

    private void exigirPendente(String operacao) {
        if (status != Status.PENDENTE) {
            throw new RegraDeNegocioViolada("OFERTA",
                    "Não dá para %s esta oferta: ela já está %s".formatted(operacao, status));
        }
    }

    public Long getId() {
        return id;
    }

    public Doacao getDoacao() {
        return doacao;
    }

    public Necessidade getNecessidade() {
        return necessidade;
    }

    public Status getStatus() {
        return status;
    }

    public OffsetDateTime getCriadaEm() {
        return criadaEm;
    }

    public OffsetDateTime getExpiraEm() {
        return expiraEm;
    }

    public OffsetDateTime getRespondidaEm() {
        return respondidaEm;
    }
}
