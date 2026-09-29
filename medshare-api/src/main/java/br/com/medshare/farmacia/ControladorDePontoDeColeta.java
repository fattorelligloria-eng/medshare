package br.com.medshare.farmacia;

import br.com.medshare.farmacia.dto.PontoDeColetaResumido;
import br.com.medshare.seguranca.UsuarioLogado;
import br.com.medshare.usuario.Endereco;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pontos-de-coleta")
public class ControladorDePontoDeColeta {

    private static final int QUANTIDADE_SUGERIDA = 5;

    private final PontoDeColetaRepository pontos;
    private final UsuarioLogado usuarioLogado;

    public ControladorDePontoDeColeta(PontoDeColetaRepository pontos, UsuarioLogado usuarioLogado) {
        this.pontos = pontos;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping
    public List<PontoDeColetaResumido> listar() {
        return pontos.findByAtivoTrueOrderByNome().stream()
                .map(PontoDeColetaResumido::de)
                .toList();
    }

    /**
     * As farmacias mais proximas de quem esta pedindo. Sem coordenadas no
     * cadastro, cai na lista alfabetica — melhor uma lista completa em ordem
     * previsivel do que uma lista vazia.
     */
    @GetMapping("/proximos")
    public List<PontoDeColetaResumido> maisProximos() {
        Endereco endereco = usuarioLogado.obrigatorio().getEndereco();
        if (!endereco.temCoordenadas()) {
            return listar();
        }
        return pontos.maisProximosDe(endereco.getLatitude(), endereco.getLongitude(),
                        QUANTIDADE_SUGERIDA).stream()
                .map(PontoDeColetaResumido::de)
                .toList();
    }
}
