package br.com.medshare.usuario;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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

    /** Ultima troca de senha; tokens emitidos antes dela deixam de valer. */
    @Column(name = "senha_alterada_em")
    private OffsetDateTime senhaAlteradaEm;

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

    /**
     * Atualiza o que a pessoa pode mudar sozinha.
     *
     * CPF e e-mail ficam de fora de proposito: o CPF e a identidade dela no
     * sistema, e o e-mail e como ela entra. Trocar qualquer um dos dois e outra
     * operacao, com conferencia propria — nao um campo no meio do formulario.
     */
    public void atualizarCadastro(String nome, String telefone, Endereco endereco) {
        this.nome = nome;
        this.telefone = telefone;
        this.endereco = endereco;
    }

    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
        // Em segundos inteiros, como o "emitido em" do token: o token novo,
        // emitido logo depois, cai no mesmo segundo e continua valendo.
        this.senhaAlteradaEm = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Um token emitido antes da ultima troca de senha nao vale mais. E o que
     * derruba a sessao aberta em outro aparelho quando a pessoa troca a senha
     * por desconfiar que alguem entrou na conta.
     */
    public boolean aceitaTokenEmitidoEm(Instant emitidoEm) {
        return senhaAlteradaEm == null
                || (emitidoEm != null && !emitidoEm.isBefore(senhaAlteradaEm.toInstant()));
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
