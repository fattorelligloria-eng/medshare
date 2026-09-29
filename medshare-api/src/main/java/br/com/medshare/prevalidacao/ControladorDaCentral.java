package br.com.medshare.prevalidacao;

import br.com.medshare.doacao.*;
import br.com.medshare.doacao.dto.DoacaoResumida;
import br.com.medshare.prevalidacao.dto.CasoDaCentral;
import br.com.medshare.prevalidacao.dto.PedidoDeRevisao;
import br.com.medshare.necessidade.ServicoDeNecessidade;
import br.com.medshare.integracao.RepositorioDeFotos;
import br.com.medshare.seguranca.UsuarioLogado;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RN10 - a mesa da central: o que a IA nao pode decidir sozinha chega aqui.
 */
@RestController
@RequestMapping("/api/central")
@PreAuthorize("hasRole('ADMIN')")
public class ControladorDaCentral {

    private final DoacaoRepository doacoes;
    private final AnalisePreValidacaoRepository analises;
    private final ServicoDeDoacao servico;
    private final ServicoDeNecessidade servicoDeNecessidade;
    private final UsuarioLogado usuarioLogado;
    private final RepositorioDeFotos fotos;

    public ControladorDaCentral(DoacaoRepository doacoes, AnalisePreValidacaoRepository analises,
                                ServicoDeDoacao servico, ServicoDeNecessidade servicoDeNecessidade,
                                UsuarioLogado usuarioLogado, RepositorioDeFotos fotos) {
        this.fotos = fotos;
        this.doacoes = doacoes;
        this.analises = analises;
        this.servico = servico;
        this.servicoDeNecessidade = servicoDeNecessidade;
        this.usuarioLogado = usuarioLogado;
    }

    /** UC10 passo 1 - ordenada por tempo de espera: quem espera ha mais tempo primeiro. */
    @GetMapping("/fila")
    @Transactional(readOnly = true)
    public Page<CasoDaCentral> fila(Pageable pagina) {
        return doacoes
                .findByStatusOrderByAtualizadoEmAsc(StatusDoacao.EM_ANALISE_CENTRAL, pagina)
                .map(this::caso);
    }

    @GetMapping("/casos/{codigo}")
    @Transactional(readOnly = true)
    public CasoDaCentral detalhar(@PathVariable String codigo) {
        return caso(servico.buscarPorCodigo(codigo));
    }

    /** UC10 passo 2 - a ultima leitura da foto (a doacao pode ter recebido foto nova). */
    private CasoDaCentral caso(Doacao doacao) {
        return CasoDaCentral.de(doacao,
                analises.findFirstByDoacaoIdOrderByCriadoEmDesc(doacao.getId()).orElse(null),
                fotos.urlAssinada(doacao.getFotoUrl()));
    }

    /**
     * RN08 - confirma à mão o NIS de quem a consulta automática não encontrou.
     *
     * A consulta oficial só enxerga quem recebe Bolsa Família; quem está no
     * CadÚnico sem receber precisa desta porta. Sem ela, o critério oficial
     * excluiria gente que ele mesmo deveria incluir.
     */
    @PostMapping("/cadunico/{beneficiarioId}/confirmacao")
    public Map<String, Object> confirmarCadUnico(@PathVariable Long beneficiarioId) {
        var verificacao = servicoDeNecessidade.confirmarNaCentral(
                beneficiarioId, usuarioLogado.obrigatorio());
        return Map.of(
                "confirmado", verificacao.isConfirmado(),
                "validoAte", verificacao.getValidoAte(),
                "fonte", verificacao.getFonte());
    }

    @PostMapping("/casos/{codigo}/decisao")
    public DoacaoResumida decidir(@PathVariable String codigo,
                                  @Valid @RequestBody PedidoDeRevisao pedido) {
        return DoacaoResumida.de(servico.decidirNaCentral(codigo, pedido.decisao(),
                pedido.justificativa(), usuarioLogado.obrigatorio()));
    }
}
