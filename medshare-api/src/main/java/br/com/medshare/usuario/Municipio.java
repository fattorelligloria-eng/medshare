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

    /** O codigo IBGE chega pelo ViaCEP na primeira vez que um CEP do municipio e consultado. */
    public void registrarCodigoIbge(String codigo) {
        if (this.codigoIbge == null && codigo != null && codigo.matches("\\d{7}")) {
            this.codigoIbge = codigo;
        }
    }

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
