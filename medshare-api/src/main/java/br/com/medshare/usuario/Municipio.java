package br.com.medshare.usuario;

import jakarta.persistence.*;

/**
 * RN09 - so existem os 39 municipios da Grande Sao Paulo nesta tabela.
 * Como o endereco aponta para aqui por chave estrangeira, um endereco fora da
 * regiao simplesmente nao consegue ser gravado.
 */
@Entity
@Table(name = "municipio_rmsp")
public class Municipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(name = "codigo_ibge")
    private String codigoIbge;

    protected Municipio() { }

    public Short getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCodigoIbge() {
        return codigoIbge;
    }
}
