package br.com.medshare.necessidade;

import br.com.medshare.integracao.RepositorioDeFotos;
import br.com.medshare.necessidade.dto.*;
import br.com.medshare.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/necessidades")
@PreAuthorize("hasRole('BENEFICIARIO')")
public class ControladorDeNecessidade {

    private final ServicoDeNecessidade servico;
    private final RepositorioDeFotos fotos;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeNecessidade(ServicoDeNecessidade servico, RepositorioDeFotos fotos,
                                    UsuarioLogado usuarioLogado) {
        this.servico = servico;
        this.fotos = fotos;
        this.usuarioLogado = usuarioLogado;
    }

    /**
     * RN08 - precisa acontecer antes de qualquer reserva.
     *
     * Unico ponto desta classe aberto a quem ainda nao e beneficiario, e por
     * isso tem @PreAuthorize proprio: e justamente aqui que alguem deixa de nao
     * ser. Quem ja doa e resolve que tambem precisa chega por esta porta sem
     * ter o papel ainda; e a confirmacao do NIS que o concede, la no servico.
     */
    @PostMapping("/cadunico")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> verificarCadUnico(
            @Valid @RequestBody PedidoDeVerificacaoCadUnico pedido) {
        var resultado = servico.verificarCadUnico(pedido.nis(), usuarioLogado.obrigatorio());
        VerificacaoCadUnico verificacao = resultado.verificacao();

        Map<String, Object> resposta = new java.util.LinkedHashMap<>();
        resposta.put("confirmado", verificacao.isConfirmado());
        resposta.put("validoAte", verificacao.getValidoAte());
        resposta.put("fonte", verificacao.getFonte());
        resposta.put("precisaDeAnaliseHumana", resultado.precisaDeAnaliseHumana());
        resposta.put("observacao", resultado.observacao());
        return resposta;
    }

    @PostMapping
    public ResponseEntity<NecessidadeResumida> registrar(
            @Valid @RequestBody PedidoDeNecessidade pedido) {
        Necessidade necessidade =
                servico.registrar(pedido.medicamentoId(), usuarioLogado.obrigatorio());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NecessidadeResumida.de(necessidade));
    }

    @GetMapping
    public List<NecessidadeResumida> minhasNecessidades() {
        return servico.ativasDe(usuarioLogado.obrigatorio()).stream()
                .map(NecessidadeResumida::de)
                .toList();
    }

    /** RN03 - anexa a receita ao pedido. */
    @PostMapping("/{id}/receita")
    public NecessidadeResumida anexarReceita(@PathVariable Long id,
                                             @Valid @RequestBody PedidoDeReceita pedido) {
        fotos.exigirFotoNossa(pedido.fotoUrl());
        Receita receita = servico.anexarReceita(id, pedido.fotoUrl(), pedido.dataEmissao(),
                pedido.validade(), pedido.crmMedico(), pedido.ufCrm(),
                usuarioLogado.obrigatorio());
        return NecessidadeResumida.de(receita.getNecessidade());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> encerrar(@PathVariable Long id) {
        servico.encerrar(id, usuarioLogado.obrigatorio());
        return ResponseEntity.noContent().build();
    }

    // --- UC07 A3: procuradores -------------------------------------------------

    public record ProcuradorResumido(Long id, String nome, String cpf) {
        static ProcuradorResumido de(Procurador p) {
            return new ProcuradorResumido(p.getId(), p.getNome(), p.getCpf());
        }
    }

    public record PedidoDeProcurador(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 120) String nome,
            @jakarta.validation.constraints.Pattern(regexp = "\\d{11}", message = "informe os 11 dígitos do CPF")
            String cpf) { }

    @GetMapping("/procuradores")
    public List<ProcuradorResumido> procuradores() {
        return servico.procuradoresDe(usuarioLogado.obrigatorio()).stream()
                .map(ProcuradorResumido::de).toList();
    }

    @PostMapping("/procuradores")
    public ResponseEntity<ProcuradorResumido> cadastrarProcurador(@Valid @RequestBody PedidoDeProcurador pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ProcuradorResumido.de(
                servico.cadastrarProcurador(pedido.nome(), pedido.cpf(), usuarioLogado.obrigatorio())));
    }

    @DeleteMapping("/procuradores/{id}")
    public ResponseEntity<Void> removerProcurador(@PathVariable Long id) {
        servico.removerProcurador(id, usuarioLogado.obrigatorio());
        return ResponseEntity.noContent().build();
    }

    /** Conveniencia para o app: a validade de hoje, calculada no servidor. */
    @GetMapping("/hoje")
    public Map<String, LocalDate> hoje() {
        return Map.of("data", LocalDate.now());
    }
}
