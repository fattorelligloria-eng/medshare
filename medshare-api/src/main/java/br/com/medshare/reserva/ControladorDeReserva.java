package br.com.medshare.reserva;

import br.com.medshare.integracao.RepositorioDeFotos;
import br.com.medshare.reserva.dto.*;
import br.com.medshare.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UC06 e UC07. A reserva nasce do aceite de uma oferta (ControladorDeOferta);
 * aqui o beneficiario acompanha e cancela, e o balcao confere e entrega.
 */
@RestController
@RequestMapping("/api/reservas")
public class ControladorDeReserva {

    private final ServicoDeReserva servico;
    private final ReservaRepository reservas;
    private final RepositorioDeFotos fotos;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeReserva(ServicoDeReserva servico, ReservaRepository reservas,
                                RepositorioDeFotos fotos, UsuarioLogado usuarioLogado) {
        this.servico = servico;
        this.reservas = reservas;
        this.fotos = fotos;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('BENEFICIARIO')")
    public List<ReservaResumida> minhasReservas() {
        return reservas
                .findByNecessidadeBeneficiarioIdOrderByCriadaEmDesc(
                        usuarioLogado.obrigatorio().getId())
                .stream()
                .map(ReservaResumida::de)
                .toList();
    }

    @DeleteMapping("/{codigoRetirada}")
    @PreAuthorize("hasRole('BENEFICIARIO')")
    public ReservaResumida cancelar(@PathVariable String codigoRetirada) {
        return ReservaResumida.de(servico.cancelar(codigoRetirada, usuarioLogado.obrigatorio()));
    }

    /** UC07 passo 2 - o balcao ve o titular, os procuradores e a receita anexada. */
    @GetMapping("/{codigoRetirada}/conferencia")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    @Transactional(readOnly = true)
    public ConferenciaDaRetirada conferir(@PathVariable String codigoRetirada) {
        Reserva reserva = servico.buscarNoBalcao(codigoRetirada, usuarioLogado.obrigatorio());
        var receita = reserva.getNecessidade().getReceita();
        return ConferenciaDaRetirada.de(reserva,
                receita == null ? null : fotos.urlAssinada(receita.getFotoUrl()),
                servico.procuradoresDe(reserva));
    }

    /** UC07 passos 3 a 5 - RN03 aplicada aqui; A3: titular ou procurador cadastrado. */
    @PostMapping("/{codigoRetirada}/retirada")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    public ReservaResumida registrarRetirada(@PathVariable String codigoRetirada,
                                             @Valid @RequestBody PedidoDeRetirada pedido) {
        Entrega entrega = servico.registrarRetirada(codigoRetirada, pedido.receitaConferida(),
                pedido.documentoConferido(), pedido.cpfDeQuemRetira(), usuarioLogado.obrigatorio());
        return ReservaResumida.de(entrega.getReserva());
    }

    /** UC07 A1 - a receita nao bate: entrega negada, caixa volta ao estoque. */
    @PostMapping("/{codigoRetirada}/negativa")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    public ReservaResumida negar(@PathVariable String codigoRetirada,
                                 @Valid @RequestBody PedidoDeNegativa pedido) {
        return ReservaResumida.de(servico.negarEntrega(codigoRetirada, pedido.motivo(), usuarioLogado.obrigatorio()));
    }
}
