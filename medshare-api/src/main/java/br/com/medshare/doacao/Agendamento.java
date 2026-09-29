package br.com.medshare.doacao;

import br.com.medshare.farmacia.PontoDeColeta;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "agendamento")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ate dois por doacao: o original e um reagendamento (UC02 A2). */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doacao_id", nullable = false)
    private Doacao doacao;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ponto_coleta_id", nullable = false)
    private PontoDeColeta pontoDeColeta;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora;

    @Column(name = "codigo_entrega", nullable = false, length = 10)
    private String codigoEntrega;

    private Boolean compareceu;

    /** UC02 - o lembrete da vespera sai uma vez so. */
    @Column(name = "lembrete_enviado", nullable = false)
    private boolean lembreteEnviado;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Agendamento() { }

    public Agendamento(Doacao doacao, PontoDeColeta pontoDeColeta,
                       OffsetDateTime dataHora, String codigoEntrega) {
        this.doacao = doacao;
        this.pontoDeColeta = pontoDeColeta;
        this.dataHora = dataHora;
        this.codigoEntrega = codigoEntrega;
    }

    public void registrarComparecimento() {
        this.compareceu = true;
    }

    public void registrarFalta() {
        this.compareceu = false;
    }

    public void marcarLembreteEnviado() {
        this.lembreteEnviado = true;
    }

    public boolean isLembreteEnviado() {
        return lembreteEnviado;
    }

    public Long getId() {
        return id;
    }

    public Doacao getDoacao() {
        return doacao;
    }

    public PontoDeColeta getPontoDeColeta() {
        return pontoDeColeta;
    }

    public OffsetDateTime getDataHora() {
        return dataHora;
    }

    public String getCodigoEntrega() {
        return codigoEntrega;
    }

    public Boolean getCompareceu() {
        return compareceu;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
