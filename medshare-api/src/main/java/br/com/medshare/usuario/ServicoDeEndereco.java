package br.com.medshare.usuario;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.integracao.ConsultaDeCep;
import br.com.medshare.integracao.EnderecoConsultado;
import br.com.medshare.integracao.Geocodificador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * RN09 e Escopo "Onde" - o endereco e conferido pelo CEP e localizado no mapa.
 *
 * O ViaCEP diz a qual municipio o CEP pertence: se nao for um dos 39 da
 * Grande Sao Paulo, ou se nao bater com o municipio escolhido, o cadastro e
 * recusado. O Nominatim da as coordenadas que o matching usa para medir
 * distancia. As duas APIs sao publicas; se estiverem fora do ar, o cadastro
 * segue com o municipio escolhido (a tabela da RN09 continua valendo pela
 * chave estrangeira) e sem coordenadas.
 */
@Service
public class ServicoDeEndereco {

    private final MunicipioRepository municipios;
    private final ConsultaDeCep consultaDeCep;
    private final Geocodificador geocodificador;

    public ServicoDeEndereco(MunicipioRepository municipios, ConsultaDeCep consultaDeCep,
                             Geocodificador geocodificador) {
        this.municipios = municipios;
        this.consultaDeCep = consultaDeCep;
        this.geocodificador = geocodificador;
    }

    /** O que a tela de cadastro usa para preencher o endereco a partir do CEP. */
    public record EnderecoPeloCep(EnderecoConsultado endereco, Short municipioId, boolean atendido) { }

    @Transactional
    public Optional<EnderecoPeloCep> consultar(String cep) {
        return consultaDeCep.porCep(cep).map(consultado -> {
            Optional<Municipio> municipio = municipioDaRegiao(consultado);
            municipio.ifPresent(m -> m.registrarCodigoIbge(consultado.codigoIbge()));
            return new EnderecoPeloCep(consultado, municipio.map(Municipio::getId).orElse(null),
                    municipio.isPresent());
        });
    }

    /**
     * Monta o endereco conferido. Coordenadas enviadas pelo cliente (GPS do
     * aparelho) tem preferencia; sem elas, o Nominatim localiza.
     */
    @Transactional
    public Endereco montar(String cep, String logradouro, String numero, String complemento,
                           String bairro, Short municipioId, Double latitude, Double longitude) {
        Municipio escolhido = municipios.findById(municipioId)
                .orElseThrow(() -> new RegraDeNegocioViolada("RN09",
                        "O MedShare atende apenas os 39 municípios da Grande São Paulo"));

        consultaDeCep.porCep(cep).ifPresent(consultado -> {
            Municipio doCep = municipioDaRegiao(consultado).orElseThrow(() -> new RegraDeNegocioViolada("RN09",
                    "O CEP %s é de %s/%s, fora da Grande São Paulo. O MedShare atende só os 39 municípios da região."
                            .formatted(cep, consultado.municipio(), consultado.uf())));
            if (!doCep.getId().equals(escolhido.getId())) {
                throw new RegraDeNegocioViolada("RN09",
                        "O CEP %s é de %s, mas o município escolhido foi %s. Confira o endereço."
                                .formatted(cep, doCep.getNome(), escolhido.getNome()));
            }
            doCep.registrarCodigoIbge(consultado.codigoIbge());
        });

        if (latitude == null || longitude == null) {
            var coordenadas = geocodificador.localizar(logradouro, numero, escolhido.getNome(), cep);
            latitude = coordenadas.map(Geocodificador.Coordenadas::latitude).orElse(null);
            longitude = coordenadas.map(Geocodificador.Coordenadas::longitude).orElse(null);
        }
        return new Endereco(cep, logradouro, numero, complemento, bairro, escolhido, latitude, longitude);
    }

    /** O municipio do CEP na tabela da RN09: pelo codigo IBGE, ou pelo nome com UF SP. */
    private Optional<Municipio> municipioDaRegiao(EnderecoConsultado consultado) {
        if (!"SP".equalsIgnoreCase(consultado.uf())) {
            return Optional.empty();
        }
        if (consultado.codigoIbge() != null) {
            Optional<Municipio> porCodigo = municipios.findByCodigoIbge(consultado.codigoIbge());
            if (porCodigo.isPresent()) {
                return porCodigo;
            }
        }
        return municipios.findByNomeIgnoreCase(consultado.municipio());
    }
}
