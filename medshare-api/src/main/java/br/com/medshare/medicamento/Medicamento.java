package br.com.medshare.medicamento;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Catalogo alimentado pela tabela CMED/ANVISA.
 *
 * O PMC (Preco Maximo ao Consumidor) e o que define "alto custo" no projeto:
 * o criterio nao foi inventado pelo grupo, ele vem da tabela oficial.
 */
@Entity
@Table(name = "medicamento")
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Um registro tem varias apresentacoes; nao e unico. */
    @Column(name = "registro_anvisa", nullable = false)
    private String registroAnvisa;

    /**
     * Codigo da apresentacao na lista da CMED, unico. E por ele que a
     * importacao sabe se atualiza uma linha existente ou cria uma nova.
     * Nulo nos medicamentos cadastrados a mao (dados de demonstracao).
     */
    @Column(name = "codigo_ggrem", unique = true, length = 15)
    private String codigoGgrem;

    @Column(name = "nome_comercial", nullable = false)
    private String nomeComercial;

    @Column(name = "principio_ativo", nullable = false)
    private String principioAtivo;

    @Column(nullable = false)
    private String apresentacao;

    private String laboratorio;

    private String ean;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pmc;

    @Column(name = "vigencia_cmed", nullable = false)
    private LocalDate vigenciaCmed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tarja tarja = Tarja.VERMELHA;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    /**
     * Coluna calculada pelo banco (GENERATED ALWAYS AS pmc >= 150).
     * A aplicacao le, nunca escreve: assim nao existe a possibilidade de o
     * campo ficar dessincronizado do preco.
     */
    @Generated
    @Column(name = "alto_custo", insertable = false, updatable = false)
    private Boolean altoCusto;

    protected Medicamento() { }

    public Medicamento(String registroAnvisa, String nomeComercial, String principioAtivo,
                       String apresentacao, String laboratorio, String ean,
                       BigDecimal pmc, LocalDate vigenciaCmed, Tarja tarja) {
        this.registroAnvisa = registroAnvisa;
        this.nomeComercial = nomeComercial;
        this.principioAtivo = principioAtivo;
        this.apresentacao = apresentacao;
        this.laboratorio = laboratorio;
        this.ean = ean;
        this.pmc = pmc;
        this.vigenciaCmed = vigenciaCmed;
        this.tarja = tarja;
    }

    /**
     * RN07 - so entra na rede medicamento com PMC igual ou acima do piso.
     * O piso vem da configuracao para nao virar um numero magico no meio do codigo.
     */
    public boolean atendeAoPisoDePreco(BigDecimal pisoEmReais) {
        return pmc.compareTo(pisoEmReais) >= 0;
    }

    public void atualizarPreco(BigDecimal novoPmc, LocalDate novaVigencia) {
        this.pmc = novoPmc;
        this.vigenciaCmed = novaVigencia;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public String descricaoCompleta() {
        return "%s (%s) - %s".formatted(nomeComercial, principioAtivo, apresentacao);
    }

    public Long getId() {
        return id;
    }

    public String getCodigoGgrem() {
        return codigoGgrem;
    }

    public String getRegistroAnvisa() {
        return registroAnvisa;
    }

    public String getNomeComercial() {
        return nomeComercial;
    }

    public String getPrincipioAtivo() {
        return principioAtivo;
    }

    public String getApresentacao() {
        return apresentacao;
    }

    public String getLaboratorio() {
        return laboratorio;
    }

    public String getEan() {
        return ean;
    }

    public BigDecimal getPmc() {
        return pmc;
    }

    public LocalDate getVigenciaCmed() {
        return vigenciaCmed;
    }

    public Tarja getTarja() {
        return tarja;
    }

    public Boolean getAltoCusto() {
        return altoCusto;
    }
}
