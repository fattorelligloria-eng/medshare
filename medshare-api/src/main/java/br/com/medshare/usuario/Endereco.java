package br.com.medshare.usuario;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Endereco usado tanto pelo usuario quanto pelo ponto de coleta.
 * As coordenadas vem do ViaCEP + Nominatim e sao o que permite ao matching
 * escolher a farmacia mais proxima.
 */
@Embeddable
public class Endereco {

    @NotBlank
    @Pattern(regexp = "\\d{8}", message = "o CEP deve ter 8 dígitos, sem traço")
    @Column(nullable = false, length = 8)
    private String cep;

    @NotBlank
    @Column(nullable = false)
    private String logradouro;

    @NotBlank
    @Column(nullable = false)
    private String numero;

    private String complemento;

    @NotBlank
    @Column(nullable = false)
    private String bairro;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "municipio_id", nullable = false)
    private Municipio municipio;   // RN09

    private Double latitude;
    private Double longitude;

    protected Endereco() { }

    public Endereco(String cep, String logradouro, String numero, String complemento,
                    String bairro, Municipio municipio, Double latitude, Double longitude) {
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.municipio = municipio;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public boolean temCoordenadas() {
        return latitude != null && longitude != null;
    }

    public String getCep() {
        return cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public Municipio getMunicipio() {
        return municipio;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
