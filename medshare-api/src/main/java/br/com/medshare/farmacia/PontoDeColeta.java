package br.com.medshare.farmacia;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.usuario.Endereco;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Farmacia parceira.
 *
 * RN05 - doador e beneficiario nunca se encontram. O ponto de coleta e o
 * intermediario obrigatorio: o doador entrega aqui, o beneficiario retira aqui,
 * em momentos diferentes.
 *
 * UC08 - o ponto nasce inativo e so passa a aparecer para doacao e retirada
 * quando tem um farmaceutico responsavel com CRF.
 *
 * UC02 - o horario de funcionamento e estruturado (dias, abertura, fechamento
 * e vagas por hora) para o app oferecer so os horarios em que da para entregar.
 */
@Entity
@Table(name = "ponto_coleta")
public class PontoDeColeta {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Embedded
    private Endereco endereco;

    /** Texto para exibir; derivado dos campos abaixo. */
    @Column(nullable = false)
    private String horario;

    @Column(name = "abre_as", nullable = false)
    private LocalTime abreAs = LocalTime.of(8, 0);

    @Column(name = "fecha_as", nullable = false)
    private LocalTime fechaAs = LocalTime.of(18, 0);

    /** ISO-8601: 1 = segunda ... 7 = domingo, separados por virgula. */
    @Column(name = "dias_de_funcionamento", nullable = false, length = 13)
    private String diasDeFuncionamento = "1,2,3,4,5";

    @Column(name = "vagas_por_hora", nullable = false)
    private short vagasPorHora = 4;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected PontoDeColeta() { }

    /** Horario padrao: segunda a sexta, 8h as 18h. */
    public PontoDeColeta(String nome, String cnpj, Endereco endereco, String horario) {
        this.nome = nome;
        this.cnpj = cnpj;
        this.endereco = endereco;
        this.horario = horario;
    }

    public PontoDeColeta(String nome, String cnpj, Endereco endereco, Set<DayOfWeek> dias,
                         LocalTime abreAs, LocalTime fechaAs, int vagasPorHora) {
        this(nome, cnpj, endereco, "");
        definirHorario(dias, abreAs, fechaAs, vagasPorHora);
    }

    public void definirHorario(Set<DayOfWeek> dias, LocalTime abre, LocalTime fecha, int vagas) {
        if (dias == null || dias.isEmpty()) {
            throw new RegraDeNegocioViolada("PONTO", "Informe ao menos um dia de funcionamento");
        }
        if (abre == null || fecha == null || !fecha.isAfter(abre)) {
            throw new RegraDeNegocioViolada("PONTO", "O horário de fechamento precisa ser depois da abertura");
        }
        if (vagas < 1) {
            throw new RegraDeNegocioViolada("PONTO", "A farmácia precisa receber ao menos 1 entrega por hora");
        }
        this.diasDeFuncionamento = EnumSet.copyOf(dias).stream()
                .map(d -> String.valueOf(d.getValue())).collect(Collectors.joining(","));
        this.abreAs = abre;
        this.fechaAs = fecha;
        this.vagasPorHora = (short) vagas;
        this.horario = descreverHorario();
    }

    public Set<DayOfWeek> getDias() {
        return Arrays.stream(diasDeFuncionamento.split(","))
                .map(d -> DayOfWeek.of(Integer.parseInt(d.trim())))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    /** O horario cai num dia e numa hora em que a farmacia atende? */
    public boolean funcionaEm(OffsetDateTime momento) {
        ZonedDateTime local = momento.atZoneSameInstant(FUSO);
        LocalTime hora = local.toLocalTime();
        return getDias().contains(local.getDayOfWeek())
                && !hora.isBefore(abreAs) && hora.isBefore(fechaAs);
    }

    /** "Segunda a sexta, 8h às 18h" — o texto que aparece no app. */
    String descreverHorario() {
        Set<DayOfWeek> dias = getDias();
        String quando;
        if (dias.equals(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))) {
            quando = "Segunda a sexta";
        } else if (dias.equals(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY))) {
            quando = "Segunda a sábado";
        } else if (dias.size() == 7) {
            quando = "Todos os dias";
        } else {
            quando = dias.stream()
                    .map(d -> d.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("pt-BR")))
                    .collect(Collectors.joining(", "));
            quando = Character.toUpperCase(quando.charAt(0)) + quando.substring(1);
        }
        return "%s, %s às %s".formatted(quando, hora(abreAs), hora(fechaAs));
    }

    private static String hora(LocalTime t) {
        return t.getMinute() == 0 ? t.getHour() + "h" : "%dh%02d".formatted(t.getHour(), t.getMinute());
    }

    public void ativar() {
        this.ativo = true;
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

    public LocalTime getAbreAs() {
        return abreAs;
    }

    public LocalTime getFechaAs() {
        return fechaAs;
    }

    public int getVagasPorHora() {
        return vagasPorHora;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
