package br.com.medshare.farmacia;

import br.com.medshare.usuario.Usuario;
import jakarta.persistence.*;

/**
 * RN10 - a decisao final sobre uma doacao e sempre de uma pessoa com CRF.
 * A IA pre-analisa, mas quem aprova ou rejeita e o farmaceutico.
 */
@Entity
@Table(name = "farmaceutico")
public class Farmaceutico {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, unique = true)
    private String crf;

    @Column(name = "uf_crf", nullable = false, length = 2)
    private String ufCrf = "SP";

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ponto_coleta_id", nullable = false)
    private PontoDeColeta pontoDeColeta;

    protected Farmaceutico() { }

    public Farmaceutico(Usuario usuario, String crf, String ufCrf, PontoDeColeta pontoDeColeta) {
        this.usuario = usuario;
        this.crf = crf;
        this.ufCrf = ufCrf;
        this.pontoDeColeta = pontoDeColeta;
    }

    /** UC08 - o administrador pode mudar o farmaceutico de farmacia. */
    public void passarAAtuarEm(PontoDeColeta ponto, String crf, String ufCrf) {
        this.pontoDeColeta = ponto;
        this.crf = crf;
        this.ufCrf = ufCrf;
    }

    public boolean atuaEm(PontoDeColeta ponto) {
        return pontoDeColeta.getId().equals(ponto.getId());
    }

    public String registroFormatado() {
        return "CRF-%s %s".formatted(ufCrf, crf);
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getCrf() {
        return crf;
    }

    public String getUfCrf() {
        return ufCrf;
    }

    public PontoDeColeta getPontoDeColeta() {
        return pontoDeColeta;
    }
}
