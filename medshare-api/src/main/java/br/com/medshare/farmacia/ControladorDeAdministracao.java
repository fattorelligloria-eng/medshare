package br.com.medshare.farmacia;

import br.com.medshare.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** UC08 - gestao da rede de farmacias parceiras, so para o administrador. */
@RestController
@RequestMapping("/api/admin/pontos")
@PreAuthorize("hasRole('ADMIN')")
public class ControladorDeAdministracao {

    private final ServicoDeAdministracao servico;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeAdministracao(ServicoDeAdministracao servico, UsuarioLogado usuarioLogado) {
        this.servico = servico;
        this.usuarioLogado = usuarioLogado;
    }

    public record PontoAdministrado(Long id, String nome, String cnpj, String endereco, String municipio,
                                    String horario, List<Integer> dias, LocalTime abreAs, LocalTime fechaAs,
                                    int vagasPorHora, boolean ativo, List<FarmaceuticoResumido> farmaceuticos) { }

    public record FarmaceuticoResumido(String nome, String email, String crf) { }

    public record PedidoDePonto(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{14}", message = "informe os 14 dígitos do CNPJ") String cnpj,
            @NotBlank @Pattern(regexp = "\\d{8}", message = "informe os 8 dígitos do CEP") String cep,
            @NotBlank @Size(max = 160) String logradouro,
            @NotBlank @Size(max = 20) String numero,
            @Size(max = 60) String complemento,
            @NotBlank @Size(max = 80) String bairro,
            @NotNull Short municipioId,
            Double latitude,
            Double longitude,
            @NotEmpty List<@Min(1) @Max(7) Integer> dias,
            @NotNull LocalTime abreAs,
            @NotNull LocalTime fechaAs,
            @Min(1) @Max(50) int vagasPorHora) { }

    public record PedidoDeHorario(@NotEmpty List<@Min(1) @Max(7) Integer> dias, @NotNull LocalTime abreAs,
                                  @NotNull LocalTime fechaAs, @Min(1) @Max(50) int vagasPorHora) { }

    public record PedidoDeFarmaceutico(@NotBlank @Email String email,
                                       @NotBlank @Size(max = 8) String crf,
                                       @NotBlank @Size(min = 2, max = 2) String ufCrf) { }

    public record PedidoDeTransferencia(@NotNull Long destinoId) { }

    @GetMapping
    @Transactional(readOnly = true)
    public List<PontoAdministrado> listar() {
        return servico.todosOsPontos().stream().map(this::resumir).toList();
    }

    /** UC08 passo 1 - a farmacia nasce inativa. */
    @PostMapping
    @Transactional
    public ResponseEntity<PontoAdministrado> cadastrar(@Valid @RequestBody PedidoDePonto p) {
        PontoDeColeta ponto = servico.cadastrarPonto(new ServicoDeAdministracao.DadosDoPonto(
                p.nome(), p.cnpj(), p.cep(), p.logradouro(), p.numero(), p.complemento(), p.bairro(),
                p.municipioId(), p.latitude(), p.longitude(), dias(p.dias()), p.abreAs(), p.fechaAs(),
                p.vagasPorHora()));
        return ResponseEntity.status(HttpStatus.CREATED).body(resumir(ponto));
    }

    @PutMapping("/{id}/horario")
    @Transactional
    public PontoAdministrado alterarHorario(@PathVariable Long id, @Valid @RequestBody PedidoDeHorario p) {
        return resumir(servico.alterarHorario(id, dias(p.dias()), p.abreAs(), p.fechaAs(), p.vagasPorHora()));
    }

    /** UC08 passo 2 - farmaceutico responsavel com CRF; a farmacia e ativada. */
    @PostMapping("/{id}/farmaceuticos")
    @Transactional
    public PontoAdministrado vincularFarmaceutico(@PathVariable Long id, @Valid @RequestBody PedidoDeFarmaceutico p) {
        return resumir(servico.vincularFarmaceutico(id, p.email(), p.crf(), p.ufCrf()).getPontoDeColeta());
    }

    /** UC08 A2 - o estoque disponivel vai para outra farmacia. */
    @PostMapping("/{id}/transferencia")
    public Map<String, Integer> transferir(@PathVariable Long id, @Valid @RequestBody PedidoDeTransferencia p) {
        return Map.of("transferidas", servico.transferirEstoque(id, p.destinoId(), usuarioLogado.obrigatorio()));
    }

    @PostMapping("/{id}/desativacao")
    @Transactional
    public PontoAdministrado desativar(@PathVariable Long id) {
        return resumir(servico.desativar(id));
    }

    private PontoAdministrado resumir(PontoDeColeta ponto) {
        var e = ponto.getEndereco();
        return new PontoAdministrado(ponto.getId(), ponto.getNome(), ponto.getCnpj(),
                "%s, %s - %s".formatted(e.getLogradouro(), e.getNumero(), e.getBairro()),
                e.getMunicipio().getNome(), ponto.getHorario(),
                ponto.getDias().stream().map(DayOfWeek::getValue).sorted().toList(),
                ponto.getAbreAs(), ponto.getFechaAs(), ponto.getVagasPorHora(), ponto.isAtivo(),
                servico.farmaceuticosDo(ponto.getId()).stream()
                        .map(f -> new FarmaceuticoResumido(f.getUsuario().getNome(),
                                f.getUsuario().getEmail(), f.registroFormatado()))
                        .toList());
    }

    private static Set<DayOfWeek> dias(List<Integer> numeros) {
        return numeros.stream().map(DayOfWeek::of).collect(Collectors.toSet());
    }
}
