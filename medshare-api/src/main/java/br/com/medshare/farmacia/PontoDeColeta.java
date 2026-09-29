package br.com.medshare.farmacia;

import br.com.medshare.usuario.Endereco;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Farmacia parceira.
 *
 * RN05 - doador e beneficiario nunca se encontram. O ponto de coleta e o
 * intermediario obrigatorio: o doador entrega aqui, o beneficiario retira aqui,
 * em momentos diferentes.
 */
@Entity
@Table(name = "ponto_coleta")
public class PontoDeColeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Embedded
    private Endereco endereco;

    @Column(nullable = false)
    private String horario;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected PontoDeColeta() { }

    public PontoDeColeta(String nome, String cnpj, Endereco endereco, String horario) {
        this.nome = nome;
        this.cnpj = cnpj;
        this.endereco = endereco;
        this.horario = horario;
    }

    public void desativar() {
        this.ativo = false;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCnpj() {
        return cnpj;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public String getHorario() {
        return horario;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
