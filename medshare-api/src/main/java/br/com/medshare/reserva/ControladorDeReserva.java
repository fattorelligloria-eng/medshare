package br.com.medshare.reserva;

import br.com.medshare.reserva.dto.*;
import br.com.medshare.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ControladorDeReserva {

    private final ServicoDeReserva servico;
    private final ReservaRepository reservas;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeReserva(ServicoDeReserva servico, ReservaRepository reservas,
                                UsuarioLogado usuarioLogado) {
        this.servico = servico;
        this.reservas = reservas;
        this.usuarioLogado = usuarioLogado;
    }

    @PostMapping
    @PreAuthorize("hasRole('BENEFICIARIO')")
    public ResponseEntity<ReservaResumida> reservar(@Valid @RequestBody PedidoDeReserva pedido) {
        Reserva reserva = servico.reservarPara(pedido.necessidadeId(), usuarioLogado.obrigatorio());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResumida.de(reserva));
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

    /** Balcao: o beneficiario chegou com o codigo. RN03 aplicada aqui. */
    @PostMapping("/{codigoRetirada}/retirada")
    @PreAuthorize("hasRole('FARMACEUTICO')")
    public ReservaResumida registrarRetirada(@PathVariable String codigoRetirada,
                                             @RequestBody PedidoDeRetirada pedido) {
        Entrega entrega = servico.registrarRetirada(codigoRetirada, pedido.receitaConferida(),
                pedido.documentoConferido(), usuarioLogado.obrigatorio());
        return ReservaResumida.de(entrega.getReserva());
    }
}
