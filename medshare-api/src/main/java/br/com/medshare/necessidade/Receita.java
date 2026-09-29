package br.com.medshare.necessidade;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * RN03 - receita medica do beneficiario.
 *
 * Dado sensivel de saude: a foto fica no R2 com URL assinada e prazo curto, e
 * esta imagem nunca e enviada para servico externo de IA. So o farmaceutico do
 * ponto de coleta abre, no momento da retirada.
 */
@Entity
@Table(name = "receita")
public class Receita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "necessidade_id", nullable = false, unique = true)
    private Necessidade necessidade;

    @Column(name = "foto_url", nullable = false)
    private String fotoUrl;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(nullable = false)
    private LocalDate validade;

    @Column(name = "crm_medico", nullable = false)
    private String crmMedico;

    @Column(name = "uf_crm", nullable = false, length = 2)
    private String ufCrm = "SP";

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected Receita() { }

    public Receita(Necessidade necessidade, String fotoUrl, LocalDate dataEmissao,
                   LocalDate validade, String crmMedico, String ufCrm) {
        this.necessidade = necessidade;
        this.fotoUrl = fotoUrl;
        this.dataEmissao = dataEmissao;
        this.validade = validade;
        this.crmMedico = crmMedico;
        this.ufCrm = ufCrm;
    }

    public boolean estaValida() {
        return !LocalDate.now().isAfter(validade);
    }

    public Long getId() {
        return id;
    }

    public Necessidade getNecessidade() {
        return necessidade;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public String getCrmMedico() {
        return crmMedico;
    }

    public String getUfCrm() {
        return ufCrm;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
