package br.com.medshare.reserva;

import br.com.medshare.reserva.dto.OfertaResumida;
import br.com.medshare.reserva.dto.ReservaResumida;
import br.com.medshare.seguranca.UsuarioLogado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** UC06 - o beneficiario ve as ofertas recebidas e aceita ou recusa. */
@RestController
@RequestMapping("/api/ofertas")
@PreAuthorize("hasRole('BENEFICIARIO')")
public class ControladorDeOferta {

    private final ServicoDeOferta servico;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeOferta(ServicoDeOferta servico, UsuarioLogado usuarioLogado) {
        this.servico = servico;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping("/minhas")
    public List<OfertaResumida> minhas() {
        return servico.doBeneficiario(usuarioLogado.obrigatorio()).stream()
                .map(OfertaResumida::de)
                .toList();
    }

    @PostMapping("/{id}/aceite")
    public ReservaResumida aceitar(@PathVariable Long id) {
        return ReservaResumida.de(servico.aceitar(id, usuarioLogado.obrigatorio()));
    }

    @PostMapping("/{id}/recusa")
    public OfertaResumida recusar(@PathVariable Long id) {
        return OfertaResumida.de(servico.recusar(id, usuarioLogado.obrigatorio()));
    }
}
