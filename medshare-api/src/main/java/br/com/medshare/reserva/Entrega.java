package br.com.medshare.reserva;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.farmacia.Farmaceutico;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * O fim do ciclo: a caixa sai com o beneficiario.
 *
 * RN03 - so existe entrega com receita conferida. O construtor recusa o objeto
 * se qualquer uma das duas conferencias nao foi feita, e o banco repete a
 * mesma restricao (entrega_exige_conferencia).
 */
@Entity
@Table(name = "entrega")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Reserva reserva;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "farmaceutico_id", nullable = false)
    private Farmaceutico farmaceutico;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private OffsetDateTime ocorridoEm = OffsetDateTime.now();

    @Column(name = "receita_conferida", nullable = false)
    private boolean receitaConferida;

    @Column(name = "documento_conferido", nullable = false)
    private boolean documentoConferido;

    /** UC07 A3 - quem apresentou o documento: o titular ou um procurador cadastrado. */
    @Column(name = "retirado_por_cpf", length = 11)
    private String retiradoPorCpf;

    @Column(name = "retirado_por_procurador", nullable = false)
    private boolean retiradoPorProcurador;

    protected Entrega() { }

    public Entrega(Reserva reserva, Farmaceutico farmaceutico, boolean receitaConferida,
                   boolean documentoConferido, String retiradoPorCpf, boolean porProcurador) {
        this(reserva, farmaceutico, receitaConferida, documentoConferido);
        this.retiradoPorCpf = retiradoPorCpf;
        this.retiradoPorProcurador = porProcurador;
    }

    public String getRetiradoPorCpf() {
        return retiradoPorCpf;
    }

    public boolean isRetiradoPorProcurador() {
        return retiradoPorProcurador;
    }

    public Entrega(Reserva reserva, Farmaceutico farmaceutico,
                   boolean receitaConferida, boolean documentoConferido) {
        if (!receitaConferida) {
            throw new RegraDeNegocioViolada("RN03",
                    "A entrega exige conferência da receita médica");
        }
        if (!documentoConferido) {
            throw new RegraDeNegocioViolada("RN03",
                    "A entrega exige conferência do documento do beneficiário");
        }
        this.reserva = reserva;
        this.farmaceutico = farmaceutico;
        this.receitaConferida = true;
        this.documentoConferido = true;
    }

    public Long getId() {
        return id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public Farmaceutico getFarmaceutico() {
        return farmaceutico;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }

    public boolean isReceitaConferida() {
        return receitaConferida;
    }

    public boolean isDocumentoConferido() {
        return documentoConferido;
    }
}
