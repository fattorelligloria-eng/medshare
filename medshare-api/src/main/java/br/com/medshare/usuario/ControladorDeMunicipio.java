package br.com.medshare.usuario;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * RN09 - a lista que o app usa no combo de cadastro. Publica de proposito:
 * e preciso conhecer os municipios atendidos antes de ter conta.
 */
@RestController
@RequestMapping("/api/municipios")
public class ControladorDeMunicipio {

    private final MunicipioRepository municipios;

    public ControladorDeMunicipio(MunicipioRepository municipios) {
        this.municipios = municipios;
    }

    public record MunicipioResumido(Short id, String nome) { }

    @GetMapping
    public List<MunicipioResumido> listar() {
        return municipios.findAll().stream()
                .sorted(Comparator.comparing(Municipio::getNome))
                .map(municipio -> new MunicipioResumido(municipio.getId(), municipio.getNome()))
                .toList();
    }
}
