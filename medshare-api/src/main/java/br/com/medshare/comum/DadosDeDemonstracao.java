package br.com.medshare.comum;

import br.com.medshare.doacao.ServicoDeDoacao;
import br.com.medshare.farmacia.*;
import br.com.medshare.prevalidacao.RevisaoCentral;
import br.com.medshare.medicamento.*;
import br.com.medshare.usuario.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Carga inicial para desenvolvimento e apresentacao.
 *
 * Roda apenas com o perfil "demo" ligado:
 *     ./gradlew bootRun --args='--spring.profiles.active=demo'
 *
 * Nao vai junto no perfil padrao de proposito — dado de mentira em banco de
 * producao e problema, nao conveniencia.
 *
 * Os precos sao os tetos da propria tabela CMED/ANVISA, e nao numeros
 * inventados: o catalogo de verdade entra pelo ImportadorCmed, lendo o CSV
 * publicado pela ANVISA.
 */
@Component
@Profile("demo")
public class DadosDeDemonstracao implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDeDemonstracao.class);
    private static final String SENHA_DE_TESTE = "medshare123";

    private final UsuarioRepository usuarios;
    private final MunicipioRepository municipios;
    private final MedicamentoRepository medicamentos;
    private final PontoDeColetaRepository pontos;
    private final FarmaceuticoRepository farmaceuticos;
    private final PasswordEncoder codificador;
    private final ServicoDeDoacao doacoes;

    public DadosDeDemonstracao(UsuarioRepository usuarios, MunicipioRepository municipios,
                               MedicamentoRepository medicamentos, PontoDeColetaRepository pontos,
                               FarmaceuticoRepository farmaceuticos, PasswordEncoder codificador,
                               ServicoDeDoacao doacoes) {
        this.usuarios = usuarios;
        this.municipios = municipios;
        this.medicamentos = medicamentos;
        this.pontos = pontos;
        this.farmaceuticos = farmaceuticos;
        this.codificador = codificador;
        this.doacoes = doacoes;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarios.count() > 0) {
            log.info("Banco ja tem dados; a carga de demonstracao foi pulada.");
            return;
        }
        criarMedicamentos();
        PontoDeColeta centro = criarPontosDeColeta();
        criarPessoas(centro);
        criarDoacoesEmVariosEstagios(centro);
        log.info("Dados de demonstracao carregados. Senha de todas as contas: {}", SENHA_DE_TESTE);
    }

    private void criarMedicamentos() {
        medicamentos.save(new Medicamento("125518005", "Alecensa", "Alectinibe",
                "150 mg - 224 cápsulas", "Roche", "7896226104058",
                new BigDecimal("35000.00"), LocalDate.of(2026, 3, 31), Tarja.VERMELHA));

        medicamentos.save(new Medicamento("158900012", "Sovaldi", "Sofosbuvir",
                "400 mg - 28 comprimidos", "Gilead", "7896422521178",
                new BigDecimal("21638.00"), LocalDate.of(2026, 3, 31), Tarja.VERMELHA));

        medicamentos.save(new Medicamento("134090023", "Tivicay", "Dolutegravir",
                "50 mg - 30 comprimidos", "ViiV", "7896422518239",
                new BigDecimal("1600.00"), LocalDate.of(2026, 3, 31), Tarja.VERMELHA));

        // Abaixo do piso de R$ 150: entra no catalogo, mas a RN07 impede doacao.
        // Serve para demonstrar a regra funcionando na apresentacao.
        medicamentos.save(new Medicamento("100430180", "Dipirona Sódica", "Dipirona sódica",
                "500 mg - 10 comprimidos", "Genérico", "7891058001094",
                new BigDecimal("12.90"), LocalDate.of(2026, 3, 31), Tarja.SEM_TARJA));
    }

    private PontoDeColeta criarPontosDeColeta() {
        PontoDeColeta centro = pontos.save(new PontoDeColeta(
                "Farmácia Parceira Sé", "11222333000181",
                endereco("01001000", "Praça da Sé", "100", "Sé", "São Paulo",
                        -23.5505, -46.6339),
                "Segunda a sexta, 8h às 18h"));

        pontos.save(new PontoDeColeta(
                "Farmácia Parceira Guarulhos", "11222333000262",
                endereco("07010000", "Rua Dom Pedro II", "250", "Centro", "Guarulhos",
                        -23.4543, -46.5337),
                "Segunda a sábado, 8h às 20h"));

        pontos.save(new PontoDeColeta(
                "Farmácia Parceira Santo André", "11222333000343",
                endereco("09010000", "Rua Coronel Oliveira Lima", "80", "Centro", "Santo André",
                        -23.6639, -46.5383),
                "Segunda a sexta, 9h às 19h"));

        pontos.save(new PontoDeColeta(
                "Farmácia Parceira Osasco", "11222333000424",
                endereco("06010000", "Rua Antônio Agu", "400", "Centro", "Osasco",
                        -23.5324, -46.7916),
                "Segunda a sexta, 8h às 18h"));

        return centro;
    }

    private void criarPessoas(PontoDeColeta ondeOFarmaceuticoAtua) {
        criarUsuario("Ana Doadora", "11122233344", "ana@medshare.test",
                Set.of(Papel.DOADOR), -23.5614, -46.6558);

        criarUsuario("Bruno Beneficiário", "22233344455", "bruno@medshare.test",
                Set.of(Papel.BENEFICIARIO), -23.5450, -46.6400);

        Usuario farmaceutico = criarUsuario("Carla Farmacêutica", "33344455566",
                "carla@medshare.test", Set.of(Papel.FARMACEUTICO), -23.5505, -46.6339);
        farmaceuticos.save(new Farmaceutico(farmaceutico, "12345", "SP", ondeOFarmaceuticoAtua));

        criarUsuario("Diego Analista", "44455566677", "diego@medshare.test",
                Set.of(Papel.ADMIN), -23.5505, -46.6339);
    }

    /**
     * Deixa uma doacao parada em cada etapa do ciclo, para que as telas do
     * painel tenham o que mostrar assim que a aplicacao sobe: uma na fila da
     * central, uma aguardando o recebimento e uma aguardando a conferencia.
     *
     * Passa pelo ServicoDeDoacao de proposito, e nao inserindo linha no banco:
     * assim os dados de demonstracao respeitam as mesmas regras que um usuario
     * de verdade respeitaria, historico incluso.
     */
    private void criarDoacoesEmVariosEstagios(PontoDeColeta centro) {
        Usuario ana = usuarios.findByEmail("ana@medshare.test").orElseThrow();
        Usuario carla = usuarios.findByEmail("carla@medshare.test").orElseThrow();
        Usuario diego = usuarios.findByEmail("diego@medshare.test").orElseThrow();
        Long alecensa = medicamentos.findByRegistroAnvisa("125518005").orElseThrow().getId();
        Long sovaldi = medicamentos.findByRegistroAnvisa("158900012").orElseThrow().getId();
        Long tivicay = medicamentos.findByRegistroAnvisa("134090023").orElseThrow().getId();

        // 1) Fica na fila da central: o avaliador simulado nunca aprova sozinho.
        doacoes.cadastrar(alecensa, "ALC2026A", LocalDate.now().plusMonths(8),
                "http://localhost:8080/fotos-locais/demo-alecensa.jpg", ana);

        // 2) Aprovada pela central e agendada: aguarda o recebimento no balcao.
        var aguardandoRecebimento = doacoes.cadastrar(sovaldi, "SOV2026B",
                LocalDate.now().plusMonths(10),
                "http://localhost:8080/fotos-locais/demo-sovaldi.jpg", ana);
        doacoes.decidirNaCentral(aguardandoRecebimento.getCodigo(),
                RevisaoCentral.Decisao.APROVADA, "Foto nítida, lacre de fábrica intacto", diego);
        doacoes.agendar(aguardandoRecebimento.getCodigo(), centro.getId(),
                java.time.OffsetDateTime.now().plusDays(2), ana);

        // 3) Ja recebida: aguarda a conferencia do lacre (RN01).
        var aguardandoConferencia = doacoes.cadastrar(tivicay, "TIV2026C",
                LocalDate.now().plusMonths(14),
                "http://localhost:8080/fotos-locais/demo-tivicay.jpg", ana);
        doacoes.decidirNaCentral(aguardandoConferencia.getCodigo(),
                RevisaoCentral.Decisao.APROVADA, "Leitura conferiu com o cadastro", diego);
        doacoes.agendar(aguardandoConferencia.getCodigo(), centro.getId(),
                java.time.OffsetDateTime.now().plusDays(1), ana);
        doacoes.receber(aguardandoConferencia.getCodigo(), carla);
    }

    private Usuario criarUsuario(String nome, String cpf, String email, Set<Papel> papeis,
                                 double latitude, double longitude) {
        Endereco endereco = endereco("01310100", "Avenida Paulista", "1000", "Bela Vista",
                "São Paulo", latitude, longitude);
        return usuarios.save(new Usuario(nome, cpf, email,
                codificador.encode(SENHA_DE_TESTE), "11999990000", endereco, papeis));
    }

    private Endereco endereco(String cep, String logradouro, String numero, String bairro,
                              String nomeDoMunicipio, double latitude, double longitude) {
        Municipio municipio = municipios.findByNomeIgnoreCase(nomeDoMunicipio)
                .orElseThrow(() -> new IllegalStateException(
                        "Municipio de demonstracao fora da lista da RN09: " + nomeDoMunicipio));
        return new Endereco(cep, logradouro, numero, null, bairro, municipio, latitude, longitude);
    }
}
