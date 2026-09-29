package br.com.medshare.doacao;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.farmacia.Farmaceutico;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * RN01 - a conferencia presencial do lacre.
 *
 * Os construtores sao dois metodos de fabrica com nome, em vez de um construtor
 * com um booleano "aprovada". Assim nao existe a chamada ambigua
 * `new Validacao(f, true, false, true)` — le-se `Validacao.aprovar(...)` ou
 * `Validacao.rejeitar(...)` e o sentido fica evidente.
 */
@Entity
@Table(name = "validacao")
public class Validacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doacao_id", nullable = false, unique = true)
    private Doacao doacao;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "farmaceutico_id", nullable = false)
    private Farmaceutico farmaceutico;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private OffsetDateTime ocorridoEm = OffsetDateTime.now();

    @Column(name = "lacre_integro", nullable = false)
    private boolean lacreIntegro;

    @Column(name = "dados_conferem", nullable = false)
    private boolean dadosConferem;

    @Column(nullable = false)
    private boolean aprovada;

    @Column(name = "motivo_rejeicao")
    private String motivoRejeicao;

    protected Validacao() { }

    private Validacao(Doacao doacao, Farmaceutico farmaceutico, boolean lacreIntegro,
                      boolean dadosConferem, boolean aprovada, String motivoRejeicao) {
        this.doacao = doacao;
        this.farmaceutico = farmaceutico;
        this.lacreIntegro = lacreIntegro;
        this.dadosConferem = dadosConferem;
        this.aprovada = aprovada;
        this.motivoRejeicao = motivoRejeicao;
    }

    /** RN01 - so e possivel aprovar com lacre integro e dados conferindo. */
    public static Validacao aprovar(Doacao doacao, Farmaceutico farmaceutico) {
        return new Validacao(doacao, farmaceutico, true, true, true, null);
    }

    public static Validacao rejeitar(Doacao doacao, Farmaceutico farmaceutico,
                                     boolean lacreIntegro, boolean dadosConferem, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new RegraDeNegocioViolada("RN01",
                    "Toda rejeição precisa de um motivo registrado");
        }
        return new Validacao(doacao, farmaceutico, lacreIntegro, dadosConferem, false, motivo);
    }

    public Long getId() {
        return id;
    }

    public Doacao getDoacao() {
        return doacao;
    }

    public Farmaceutico getFarmaceutico() {
        return farmaceutico;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }

    public boolean isLacreIntegro() {
        return lacreIntegro;
    }

    public boolean isDadosConferem() {
        return dadosConferem;
    }

    public boolean isAprovada() {
        return aprovada;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }
}
