package br.com.medshare.necessidade;

import br.com.medshare.usuario.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * UC07 A3 - quem pode retirar no lugar do beneficiario.
 *
 * O cadastro e previo e feito pelo proprio beneficiario no app: no balcao, o
 * farmaceutico so aceita o documento de quem esta nesta lista. Sem isso,
 * qualquer pessoa com o codigo de retirada levaria o medicamento.
 */
@Entity
@Table(name = "procurador")
public class Procurador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiario_id", nullable = false)
    private Usuario beneficiario;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Procurador() { }

    public Procurador(Usuario beneficiario, String nome, String cpf) {
        this.beneficiario = beneficiario;
        this.nome = nome;
        this.cpf = cpf;
    }

    public Long getId() {
        return id;
    }

    public Usuario getBeneficiario() {
        return beneficiario;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
