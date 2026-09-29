package br.com.medshare.prevalidacao;

import br.com.medshare.doacao.Doacao;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * O que a leitura automatica extraiu da foto da embalagem.
 *
 * Guardamos o resultado inteiro, inclusive quando ele esta errado. E esse
 * registro que vai formar o conjunto de dados da fase 2 (modelo proprio):
 * cada linha aqui, comparada com a decisao que o farmaceutico tomou depois,
 * e um exemplo rotulado de graca.
 */
@Entity
@Table(name = "analise_pre_validacao")
public class AnalisePreValidacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doacao_id", nullable = false, unique = true)
    private Doacao doacao;

    @Column(name = "ean_lido")
    private String eanLido;

    @Column(name = "lote_lido")
    private String loteLido;

    @Column(name = "validade_lida")
    private LocalDate validadeLida;

    @Enumerated(EnumType.STRING)
    @Column(name = "classe_embalagem", nullable = false)
    private ClasseEmbalagem classeEmbalagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Certeza certeza;

    private String motivo;

    /** Qual servico produziu esta analise (gemini-flash, modelo-proprio-v1...). */
    @Column(nullable = false)
    private String avaliador;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> divergencias = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected AnalisePreValidacao() { }

    public AnalisePreValidacao(Doacao doacao, String eanLido, String loteLido,
                               LocalDate validadeLida, ClasseEmbalagem classeEmbalagem,
                               Certeza certeza, String motivo, String avaliador) {
        this.doacao = doacao;
        this.eanLido = eanLido;
        this.loteLido = loteLido;
        this.validadeLida = validadeLida;
        this.classeEmbalagem = classeEmbalagem;
        this.certeza = certeza;
        this.motivo = motivo;
        this.avaliador = avaliador;
        this.divergencias = apurarDivergencias(doacao);
    }

    /**
     * Compara o que a IA leu com o que o doador digitou.
     * Campo que a IA nao conseguiu ler nao conta como divergencia — conta como
     * ausencia de informacao, que por si so ja leva o caso para a central.
     */
    private List<String> apurarDivergencias(Doacao doacao) {
        List<String> encontradas = new ArrayList<>();
        String eanCadastrado = doacao.getMedicamento().getEan();

        if (eanLido != null && eanCadastrado != null && !eanLido.equals(eanCadastrado)) {
            encontradas.add("código de barras lido (%s) difere do medicamento cadastrado (%s)"
                    .formatted(eanLido, eanCadastrado));
        }
        if (loteLido != null && !loteLido.equalsIgnoreCase(doacao.getLote())) {
            encontradas.add("lote lido (%s) difere do informado (%s)"
                    .formatted(loteLido, doacao.getLote()));
        }
        if (validadeLida != null && !validadeLida.equals(doacao.getValidade())) {
            encontradas.add("validade lida (%s) difere da informada (%s)"
                    .formatted(validadeLida, doacao.getValidade()));
        }
        return encontradas;
    }

    public boolean temDivergencia() {
        return !divergencias.isEmpty();
    }

    public boolean leuTodosOsCampos() {
        return eanLido != null && loteLido != null && validadeLida != null;
    }

    public Long getId() {
        return id;
    }

    public Doacao getDoacao() {
        return doacao;
    }

    public String getEanLido() {
        return eanLido;
    }

    public String getLoteLido() {
        return loteLido;
    }

    public LocalDate getValidadeLida() {
        return validadeLida;
    }

    public ClasseEmbalagem getClasseEmbalagem() {
        return classeEmbalagem;
    }

    public Certeza getCerteza() {
        return certeza;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getAvaliador() {
        return avaliador;
    }

    public List<String> getDivergencias() {
        return List.copyOf(divergencias);
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
