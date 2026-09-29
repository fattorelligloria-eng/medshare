package br.com.medshare.necessidade;

import br.com.medshare.usuario.Usuario;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * RN08 - criterio de quem pode receber.
 *
 * O projeto nao inventou uma regra propria de "quem e pobre": usa o CadUnico,
 * o cadastro oficial do governo federal para programas sociais. Consulta pelo
 * NIS no Portal da Transparencia, que e publico e gratuito.
 *
 * A verificacao vale 12 meses — o mesmo prazo em que o proprio CadUnico exige
 * atualizacao cadastral.
 */
@Entity
@Table(name = "verificacao_cadunico")
public class VerificacaoCadUnico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 11)
    private String nis;

    @Column(nullable = false)
    private boolean confirmado;

    @Column(name = "verificado_em", nullable = false, updatable = false)
    private OffsetDateTime verificadoEm = OffsetDateTime.now();

    @Column(name = "valido_ate", nullable = false)
    private LocalDate validoAte;

    @Column(nullable = false)
    private String fonte = "PORTAL_TRANSPARENCIA";

    /** UC04 A2 - o Portal estava fora do ar; uma rotina tenta de novo. */
    @Column(name = "consulta_pendente", nullable = false)
    private boolean consultaPendente;

    protected VerificacaoCadUnico() { }

    public VerificacaoCadUnico(Usuario usuario, String nis, boolean confirmado,
                               int mesesDeValidade, String fonte) {
        this.usuario = usuario;
        this.nis = nis;
        this.confirmado = confirmado;
        this.validoAte = LocalDate.now().plusMonths(mesesDeValidade);
        this.fonte = fonte;
    }

    /** Confirmação feita por uma pessoa da central, quando a consulta não achou. */
    public void confirmarManualmente(int mesesDeValidade) {
        this.confirmado = true;
        this.validoAte = LocalDate.now().plusMonths(mesesDeValidade);
        this.fonte = "CENTRAL";
    }

    public void marcarConsultaPendente() {
        this.consultaPendente = true;
    }

    /** Resultado da nova tentativa de consulta ao Portal (UC04 A2). */
    public void registrarNovaConsulta(boolean confirmadoAgora, int mesesDeValidade) {
        this.consultaPendente = false;
        if (confirmadoAgora) {
            this.confirmado = true;
            this.validoAte = LocalDate.now().plusMonths(mesesDeValidade);
        }
    }

    public boolean isConsultaPendente() {
        return consultaPendente;
    }

    public boolean estaVigente() {
        return confirmado && !LocalDate.now().isAfter(validoAte);
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNis() {
        return nis;
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public OffsetDateTime getVerificadoEm() {
        return verificadoEm;
    }

    public LocalDate getValidoAte() {
        return validoAte;
    }

    public String getFonte() {
        return fonte;
    }
}
