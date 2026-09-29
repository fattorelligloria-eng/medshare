package br.com.medshare.comum;

import br.com.medshare.farmacia.PontoDeColeta;
import br.com.medshare.medicamento.Medicamento;
import br.com.medshare.medicamento.Tarja;
import br.com.medshare.usuario.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Monta objetos de dominio para os testes.
 *
 * Os identificadores sao atribuidos por reflexao porque, em producao, quem os
 * gera e o banco. Preferimos isso a abrir um setter de id na entidade so para
 * agradar o teste — teste nao deve afrouxar o desenho do dominio.
 */
public final class FabricaDeTestes {

    public static final BigDecimal PISO_DE_PRECO = new BigDecimal("150.00");
    public static final int DIAS_MINIMOS = 30;

    private FabricaDeTestes() { }

    public static Municipio municipio(short id, String nome) {
        Municipio municipio = criarVazio(Municipio.class);
        ReflectionTestUtils.setField(municipio, "id", id);
        ReflectionTestUtils.setField(municipio, "nome", nome);
        return municipio;
    }

    public static Endereco endereco(Double latitude, Double longitude) {
        return new Endereco("01001000", "Praca da Se", "1", null, "Se",
                municipio((short) 1, "São Paulo"), latitude, longitude);
    }

    public static Usuario usuario(Long id, String nome, Papel... papeis) {
        Usuario usuario = new Usuario(nome, "11122233344", nome + "@teste.com", "hash",
                "11999990000", endereco(-23.5505, -46.6339), Set.of(papeis));
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    public static Medicamento medicamento(Long id, String nome, String pmc) {
        Medicamento medicamento = new Medicamento("REG" + id, nome, "principio de " + nome,
                "10 comprimidos", "Laboratorio", "7890000000000",
                new BigDecimal(pmc), LocalDate.now(), Tarja.VERMELHA);
        ReflectionTestUtils.setField(medicamento, "id", id);
        return medicamento;
    }

    public static PontoDeColeta pontoDeColeta(Long id, String nome,
                                              double latitude, double longitude) {
        PontoDeColeta ponto = new PontoDeColeta(nome, "11222333000181",
                endereco(latitude, longitude), "8h as 18h");
        ReflectionTestUtils.setField(ponto, "id", id);
        return ponto;
    }

    private static <T> T criarVazio(Class<T> tipo) {
        try {
            var construtor = tipo.getDeclaredConstructor();
            construtor.setAccessible(true);
            return construtor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nao foi possivel instanciar " + tipo, e);
        }
    }
}
