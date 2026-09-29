package br.com.medshare.medicamento;

import br.com.medshare.comum.RecursoNaoEncontrado;
import br.com.medshare.medicamento.dto.MedicamentoResumido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medicamentos")
public class ControladorDeMedicamento {

    private final MedicamentoRepository medicamentos;

    public ControladorDeMedicamento(MedicamentoRepository medicamentos) {
        this.medicamentos = medicamentos;
    }

    /**
     * RN07 - a busca so devolve medicamentos de alto custo. Quem procura
     * dipirona aqui nao encontra, e isso e a regra funcionando, nao um bug.
     */
    @GetMapping
    public Page<MedicamentoResumido> buscar(@RequestParam(defaultValue = "") String termo,
                                            Pageable pagina) {
        return medicamentos.buscarDeAltoCusto(termo, pagina).map(MedicamentoResumido::de);
    }

    @GetMapping("/{id}")
    public MedicamentoResumido detalhar(@PathVariable Long id) {
        return medicamentos.findById(id)
                .map(MedicamentoResumido::de)
                .orElseThrow(() -> new RecursoNaoEncontrado("Medicamento", id));
    }

    /** Usado pelo app quando o doador bipa o codigo de barras da caixa. */
    @GetMapping("/por-ean/{ean}")
    public MedicamentoResumido porCodigoDeBarras(@PathVariable String ean) {
        return medicamentos.findByEan(ean)
                .map(MedicamentoResumido::de)
                .orElseThrow(() -> new RecursoNaoEncontrado("Medicamento com EAN", ean));
    }
}
