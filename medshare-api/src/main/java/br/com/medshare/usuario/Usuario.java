package br.com.medshare.usuario;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    private String telefone;

    @Embedded
    private Endereco endereco;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "papel_usuario", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "papel", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Papel> papeis = EnumSet.noneOf(Papel.class);

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Usuario() { }

    public Usuario(String nome, String cpf, String email, String senhaHash,
                   String telefone, Endereco endereco, Set<Papel> papeis) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.senhaHash = senhaHash;
        this.telefone = telefone;
        this.endereco = endereco;
        // EnumSet.copyOf estoura com colecao vazia; noneOf + addAll aceita.
        this.papeis = EnumSet.noneOf(Papel.class);
        this.papeis.addAll(papeis);
    }

    public boolean temPapel(Papel papel) {
        return papeis.contains(papel);
    }

    public void adicionarPapel(Papel papel) {
        papeis.add(papel);
    }

    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
    }

    public void desativar() {
        this.ativo = false;
    }

    /**
     * Nome reduzido para aparecer no painel sem expor a pessoa inteira.
     * "Maria Aparecida da Silva" vira "Maria A. S." — o farmaceutico confere
     * contra o documento, mas o nome completo nao fica exposto na tela.
     */
    public String nomeAbreviado() {
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0];
        }
        StringBuilder abreviado = new StringBuilder(partes[0]);
        for (int i = 1; i < partes.length; i++) {
            if (partes[i].length() > 2) {
                abreviado.append(' ').append(partes[i].charAt(0)).append('.');
            }
        }
        return abreviado.toString();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getTelefone() {
        return telefone;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public Set<Papel> getPapeis() {
        return Set.copyOf(papeis);
    }

    public boolean isAtivo() {
        return ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
