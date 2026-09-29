package br.com.medshare.doacao;

import br.com.medshare.doacao.dto.*;
import br.com.medshare.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doacoes")
public class ControladorDeDoacao {

    private final ServicoDeDoacao servico;
    private final DoacaoRepository doacoes;
    private final EventoHistoricoRepository eventos;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeDoacao(ServicoDeDoacao servico, DoacaoRepository doacoes,
                               EventoHistoricoRepository eventos, UsuarioLogado usuarioLogado) {
        this.servico = servico;
        this.doacoes = doacoes;
        this.eventos = eventos;
        this.usuarioLogado = usuarioLogado;
    }

    @PostMapping
    @PreAuthorize("hasRole('DOADOR')")
    public ResponseEntity<DoacaoResumida> cadastrar(@Valid @RequestBody PedidoDeDoacao pedido) {
        Doacao doacao = servico.cadastrar(pedido.medicamentoId(), pedido.lote(),
                pedido.validade(), pedido.fotoUrl(), usuarioLogado.obrigatorio());
        return ResponseEntity.status(HttpStatus.CREATED).body(DoacaoResumida.de(doacao));
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('DOADOR')")
    public Page<DoacaoResumida> minhasDoacoes(Pageable pagina) {
        return doacoes
                .findByDoadorIdOrderByCriadoEmDesc(usuarioLogado.obrigatorio().getId(), pagina)
                .map(DoacaoResumida::de);
    }

    @GetMapping("/{codigo}")
    public DoacaoDetalhada detalhar(@PathVariable String codigo) {
        Doacao doacao = servico.buscarPorCodigo(codigo);
        return DoacaoDetalhada.de(doacao,
                eventos.findByDoacaoIdOrderByOcorridoEmAscIdAsc(doacao.getId()));
    }

    @PostMapping("/{codigo}/agendamento")
    @PreAuthorize("hasRole('DOADOR')")
    public DoacaoResumida agendar(@PathVariable String codigo,
                                  @Valid @RequestBody PedidoDeAgendamento pedido) {
        servico.agendar(codigo, pedido.pontoDeColetaId(), pedido.dataHora(),
                usuarioLogado.obrigatorio());
        return DoacaoResumida.de(servico.buscarPorCodigo(codigo));
    }

    @DeleteMapping("/{codigo}/agendamento")
    @PreAuthorize("hasRole('DOADOR')")
    public DoacaoResumida cancelarAgendamento(@PathVariable String codigo,
                                              @RequestParam(defaultValue = "cancelado pelo doador")
                                              String motivo) {
        return DoacaoResumida.de(
                servico.cancelarAgendamento(codigo, motivo, usuarioLogado.obrigatorio()));
    }

    // --- balcao da farmacia ---------------------------------------------------

    @GetMapping("/fila")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    public Page<DoacaoResumida> filaDoBalcao(@RequestParam Long pontoDeColetaId, Pageable pagina) {
        List<StatusDoacao> emAtendimento = List.of(
                StatusDoacao.AGENDADA, StatusDoacao.RECEBIDA, StatusDoacao.VALIDADA);
        return doacoes
                .findByPontoDeColetaIdAndStatusInOrderByAtualizadoEmDesc(
                        pontoDeColetaId, emAtendimento, pagina)
                .map(DoacaoResumida::de);
    }

    @PostMapping("/{codigo}/recebimento")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    public DoacaoResumida receber(@PathVariable String codigo) {
        return DoacaoResumida.de(servico.receber(codigo, usuarioLogado.obrigatorio()));
    }

    /** RN01 - conferencia do lacre aprovada: a caixa entra no estoque. */
    @PostMapping("/{codigo}/validacao")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    public DoacaoResumida validar(@PathVariable String codigo) {
        return DoacaoResumida.de(servico.validar(codigo, usuarioLogado.obrigatorio()));
    }

    @PostMapping("/{codigo}/rejeicao")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    public DoacaoResumida rejeitar(@PathVariable String codigo,
                                   @Valid @RequestBody PedidoDeRejeicao pedido) {
        return DoacaoResumida.de(servico.rejeitar(codigo, pedido.lacreIntegro(),
                pedido.dadosConferem(), pedido.motivo(), usuarioLogado.obrigatorio()));
    }
}
