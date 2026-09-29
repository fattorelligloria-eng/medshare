package br.com.medshare.reserva;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.necessidade.Necessidade;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Liga uma caixa disponivel a quem precisa dela.
 *
 * RN05 - doador e beneficiario nunca se encontram. A ligacao entre os dois
 * existe nesta tabela porque a rede precisa saber para onde a caixa foi, mas
 * nenhum endpoint da API devolve os dois lados juntos: o doador acompanha pelo
 * codigo da doacao, o beneficiario pelo codigo de retirada, e so a farmacia ve
 * os dois — nunca os nomes um do outro.
 */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_retirada", nullable = false, unique = true, length = 10)
    private String codigoRetirada;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "doacao_id", nullable = false)
    private Doacao doacao;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "necessidade_id", nullable = false)
    private Necessidade necessidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusReserva status = StatusReserva.ATIVA;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private OffsetDateTime criadaEm = OffsetDateTime.now();

    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    protected Reserva() { }

    public Reserva(String codigoRetirada, Doacao doacao, Necessidade necessidade,
                   int horasParaRetirada) {
        this.codigoRetirada = codigoRetirada;
        this.doacao = doacao;
        this.necessidade = necessidade;
        this.expiraEm = OffsetDateTime.now().plusHours(horasParaRetirada);
    }

    public boolean venceu() {
        return status == StatusReserva.ATIVA && OffsetDateTime.now().isAfter(expiraEm);
    }

    public void concluir() {
        exigirQueEstejaAtiva("concluir");
        this.status = StatusReserva.CONCLUIDA;
    }

    public void expirar() {
        exigirQueEstejaAtiva("expirar");
        this.status = StatusReserva.EXPIRADA;
    }

    public void cancelar() {
        exigirQueEstejaAtiva("cancelar");
        this.status = StatusReserva.CANCELADA;
    }

    private void exigirQueEstejaAtiva(String operacao) {
        if (status != StatusReserva.ATIVA) {
            throw new RegraDeNegocioViolada("RESERVA",
                    "Não dá para %s a reserva %s: ela já está %s"
                            .formatted(operacao, codigoRetirada, status));
        }
    }

    public Long getId() {
        return id;
    }

    public String getCodigoRetirada() {
        return codigoRetirada;
    }

    public Doacao getDoacao() {
        return doacao;
    }

    public Necessidade getNecessidade() {
        return necessidade;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public OffsetDateTime getCriadaEm() {
        return criadaEm;
    }

    public OffsetDateTime getExpiraEm() {
        return expiraEm;
    }
}
