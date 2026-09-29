package br.com.medshare.notificacao;

import br.com.medshare.usuario.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String corpo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacao tipo;

    @Column(nullable = false)
    private boolean lida = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Notificacao() { }

    public Notificacao(Usuario usuario, String titulo, String corpo, TipoNotificacao tipo) {
        this.usuario = usuario;
        this.titulo = titulo;
        this.corpo = corpo;
        this.tipo = tipo;
    }

    public void marcarComoLida() {
        this.lida = true;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getCorpo() {
        return corpo;
    }

    public TipoNotificacao getTipo() {
        return tipo;
    }

    public boolean isLida() {
        return lida;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
