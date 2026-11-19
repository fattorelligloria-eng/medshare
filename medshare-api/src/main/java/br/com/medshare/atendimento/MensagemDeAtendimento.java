package br.com.medshare.atendimento;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.usuario.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Uma mensagem da pessoa para a central, e a resposta.
 *
 * A pergunta nao muda depois de enviada, e a resposta e uma so: responder de
 * novo apagaria o que a pessoa ja leu. Se a central precisar completar, a
 * pessoa escreve outra mensagem.
 */
@Entity
@Table(name = "mensagem_atendimento")
public class MensagemDeAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private AssuntoDaMensagem assunto;

    @Column(nullable = false, updatable = false, length = 2000)
    private String texto;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(length = 2000)
    private String resposta;

    @Column(name = "respondida_em")
    private OffsetDateTime respondidaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondida_por")
    private Usuario respondidaPor;

    protected MensagemDeAtendimento() { }

    public MensagemDeAtendimento(Usuario usuario, AssuntoDaMensagem assunto, String texto) {
        this.usuario = usuario;
        this.assunto = assunto;
        this.texto = texto.strip();
    }

    public void responder(String resposta, Usuario quemResponde) {
        if (foiRespondida()) {
            throw new RegraDeNegocioViolada("ATENDIMENTO", "Esta mensagem já foi respondida.");
        }
        this.resposta = resposta.strip();
        this.respondidaEm = OffsetDateTime.now();
        this.respondidaPor = quemResponde;
    }

    public boolean foiRespondida() {
        return resposta != null;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public AssuntoDaMensagem getAssunto() {
        return assunto;
    }

    public String getTexto() {
        return texto;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getResposta() {
        return resposta;
    }

    public OffsetDateTime getRespondidaEm() {
        return respondidaEm;
    }
}
