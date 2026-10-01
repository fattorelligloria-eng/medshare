package br.com.medshare.doacao;

import br.com.medshare.doacao.dto.*;
import br.com.medshare.integracao.RepositorioDeFotos;
import br.com.medshare.seguranca.UsuarioLogado;
import br.com.medshare.usuario.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doacoes")
public class ControladorDeDoacao {

    private final ServicoDeDoacao servico;
    private final DoacaoRepository doacoes;
    private final EventoHistoricoRepository eventos;
    private final RepositorioDeFotos fotos;
    private final AgendamentoRepository agendamentos;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeDoacao(ServicoDeDoacao servico, DoacaoRepository doacoes,
                               EventoHistoricoRepository eventos, RepositorioDeFotos fotos,
                               AgendamentoRepository agendamentos, UsuarioLogado usuarioLogado) {
        this.servico = servico;
        this.doacoes = doacoes;
        this.eventos = eventos;
        this.fotos = fotos;
        this.agendamentos = agendamentos;
        this.usuarioLogado = usuarioLogado;
    }

    // --- doador ---------------------------------------------------------------

    /** UC01 - cria uma doacao por caixa (quantidade) e devolve todas. */
    @PostMapping
    @PreAuthorize("hasRole('DOADOR')")
    public ResponseEntity<List<DoacaoResumida>> cadastrar(@Valid @RequestBody PedidoDeDoacao pedido) {
        fotos.exigirFotoNossa(pedido.fotoUrl());
        List<Doacao> criadas = servico.cadastrar(pedido.medicamentoId(), pedido.lote(),
                pedido.validade(), pedido.fotoUrl(), pedido.quantidadeOuUm(),
                pedido.lacreDeclarado(), usuarioLogado.obrigatorio());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(criadas.stream().map(DoacaoResumida::de).toList());
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('DOADOR')")
    @Transactional(readOnly = true)
    public Page<DoacaoResumida> minhasDoacoes(Pageable pagina) {
        Page<Doacao> pagina1 = doacoes
                .findByDoadorIdOrderByCriadoEmDesc(usuarioLogado.obrigatorio().getId(), pagina);

        Map<Long, Agendamento> porDoacao = agendamentoDasDoacoes(pagina1.getContent());
        return pagina1.map(doacao -> DoacaoResumida.de(doacao, porDoacao.get(doacao.getId())));
    }

    /**
     * O agendamento atual de cada doacao da pagina, numa consulta so.
     *
     * O repositorio devolve do mais recente para o mais antigo; o merge mantem
     * o primeiro que chegou de cada doacao, que e justamente o atual quando
     * houve reagendamento.
     */
    private Map<Long, Agendamento> agendamentoDasDoacoes(List<Doacao> doacoesDaPagina) {
        if (doacoesDaPagina.isEmpty()) return Map.of();

        List<Long> ids = doacoesDaPagina.stream().map(Doacao::getId).toList();
        return agendamentos.findByDoacaoIdInOrderByCriadoEmDesc(ids).stream()
                .collect(Collectors.toMap(a -> a.getDoacao().getId(), a -> a,
                        (atual, antigo) -> atual));
    }

    /** RN06 / UC09 - o historico, visivel so para o doador, a farmacia e a central. */
    @GetMapping("/{codigo}")
    @Transactional(readOnly = true)
    public DoacaoDetalhada detalhar(@PathVariable String codigo) {
        Doacao doacao = servico.detalharPara(codigo, usuarioLogado.obrigatorio());
        Agendamento agendamento = servico.agendamentoAtualDe(doacao).orElse(null);
        return DoacaoDetalhada.de(doacao,
                eventos.findByDoacaoIdOrderByOcorridoEmAscIdAsc(doacao.getId()),
                agendamento, servico.podeReagendar(doacao));
    }

    /** UC02 - agenda (ou reagenda, uma vez, depois de um cancelamento). */
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
                                              @Size(max = 200) String motivo) {
        return DoacaoResumida.de(
                servico.cancelarAgendamento(codigo, motivo, usuarioLogado.obrigatorio()));
    }

    /** UC10 A2 - a foto nova que a central pediu. */
    @PostMapping("/{codigo}/foto")
    @PreAuthorize("hasRole('DOADOR')")
    public DoacaoResumida trocarFoto(@PathVariable String codigo, @Valid @RequestBody PedidoDeNovaFoto pedido) {
        fotos.exigirFotoNossa(pedido.fotoUrl());
        return DoacaoResumida.de(servico.trocarFoto(codigo, pedido.fotoUrl(), usuarioLogado.obrigatorio()));
    }

    // --- balcao da farmacia (UC03) --------------------------------------------

    /** A fila do ponto de coleta onde o farmaceutico logado atua. */
    @GetMapping("/fila")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    @Transactional(readOnly = true)
    public Page<DoacaoNoBalcao> filaDoBalcao(Pageable pagina) {
        return servico.filaDoBalcao(usuarioLogado.obrigatorio(), pagina).map(this::noBalcao);
    }

    /** UC03 passo 1 - o codigo de entrega que o doador mostra. */
    @GetMapping("/balcao/entrega/{codigoEntrega}")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    @Transactional(readOnly = true)
    public DoacaoNoBalcao porCodigoDeEntrega(@PathVariable String codigoEntrega) {
        return noBalcao(servico.buscarPorCodigoDeEntrega(codigoEntrega, usuarioLogado.obrigatorio()));
    }

    /** UC03 A3 - o doador chegou sem o codigo: busca pelo CPF ou telefone. */
    @GetMapping("/balcao/busca")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    @Transactional(readOnly = true)
    public List<DoacaoNoBalcao> porDocumentoDoDoador(@RequestParam @Size(max = 20) String documento) {
        Usuario farmaceutico = usuarioLogado.obrigatorio();
        return servico.buscarPorDocumentoDoDoador(documento, farmaceutico).stream()
                .map(this::noBalcao).toList();
    }

    @PostMapping("/{codigo}/recebimento")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    public DoacaoResumida receber(@PathVariable String codigo) {
        return DoacaoResumida.de(servico.receber(codigo, usuarioLogado.obrigatorio()));
    }

    /** RN01 - conferencia do lacre aprovada (com correcao opcional - A1): a caixa entra no estoque. */
    @PostMapping("/{codigo}/validacao")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    public DoacaoResumida validar(@PathVariable String codigo,
                                  @Valid @RequestBody(required = false) PedidoDeValidacao pedido) {
        PedidoDeValidacao dados = pedido == null ? new PedidoDeValidacao(null, null, null) : pedido;
        if (dados.fotoUrl() != null) {
            fotos.exigirFotoNossa(dados.fotoUrl());
        }
        return DoacaoResumida.de(servico.validar(codigo, dados.fotoUrl(), dados.lote(), dados.validade(),
                usuarioLogado.obrigatorio()));
    }

    @PostMapping("/{codigo}/rejeicao")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    public DoacaoResumida rejeitar(@PathVariable String codigo,
                                   @Valid @RequestBody PedidoDeRejeicao pedido) {
        if (pedido.fotoUrl() != null) {
            fotos.exigirFotoNossa(pedido.fotoUrl());
        }
        return DoacaoResumida.de(servico.rejeitar(codigo, pedido.lacreIntegro(),
                pedido.dadosConferem(), pedido.motivo(), pedido.fotoUrl(), usuarioLogado.obrigatorio()));
    }

    private DoacaoNoBalcao noBalcao(Doacao doacao) {
        return DoacaoNoBalcao.de(doacao, servico.agendamentoAtualDe(doacao).orElse(null),
                fotos.urlAssinada(doacao.getFotoUrl()));
    }
}
