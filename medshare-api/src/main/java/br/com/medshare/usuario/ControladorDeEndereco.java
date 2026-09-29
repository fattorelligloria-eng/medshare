package br.com.medshare.usuario;

import br.com.medshare.comum.RecursoNaoEncontrado;
import org.springframework.web.bind.annotation.*;

/**
 * Escopo "Onde" / RN09 - preenche o endereco a partir do CEP na tela de
 * cadastro e ja avisa se o CEP fica fora da Grande Sao Paulo. Publico, como a
 * lista de municipios: e usado antes de a pessoa ter conta.
 */
@RestController
@RequestMapping("/api/enderecos")
public class ControladorDeEndereco {

    private final ServicoDeEndereco enderecos;

    public ControladorDeEndereco(ServicoDeEndereco enderecos) {
        this.enderecos = enderecos;
    }

    public record EnderecoDoCep(String cep, String logradouro, String bairro, String municipio,
                                String uf, Short municipioId, boolean atendido) { }

    @GetMapping("/{cep}")
    public EnderecoDoCep porCep(@PathVariable String cep) {
        return enderecos.consultar(cep)
                .map(r -> new EnderecoDoCep(r.endereco().cep().replaceAll("\\D", ""),
                        r.endereco().logradouro(), r.endereco().bairro(), r.endereco().municipio(),
                        r.endereco().uf(), r.municipioId(), r.atendido()))
                .orElseThrow(() -> new RecursoNaoEncontrado("CEP", cep));
    }
}
