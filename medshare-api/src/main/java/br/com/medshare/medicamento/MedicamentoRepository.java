package br.com.medshare.medicamento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    /** O registro se repete entre apresentacoes; serve para os dados de demonstracao. */
    Optional<Medicamento> findFirstByRegistroAnvisa(String registroAnvisa);

    /** O mesmo EAN pode aparecer em mais de uma linha antiga da lista. */
    Optional<Medicamento> findFirstByEanOrderByIdDesc(String ean);

    /**
     * Busca do app: o usuario digita parte do nome ou do principio ativo.
     *
     * Ignora acento e maiusculas dos dois lados — a lista da CMED vem em
     * maiusculas acentuadas ("ÁCIDO"), e ninguem digita assim no celular.
     * A funcao sem_acento esta na migration V2.
     */
    @Query(value = """
            SELECT m.* FROM medicamento m
            WHERE m.alto_custo
              AND (sem_acento(m.nome_comercial)  LIKE '%' || sem_acento(:termo) || '%'
                OR sem_acento(m.principio_ativo) LIKE '%' || sem_acento(:termo) || '%')
            ORDER BY m.nome_comercial, m.apresentacao
            """,
            countQuery = """
            SELECT count(*) FROM medicamento m
            WHERE m.alto_custo
              AND (sem_acento(m.nome_comercial)  LIKE '%' || sem_acento(:termo) || '%'
                OR sem_acento(m.principio_ativo) LIKE '%' || sem_acento(:termo) || '%')
            """,
            nativeQuery = true)
    Page<Medicamento> buscarDeAltoCusto(@Param("termo") String termo, Pageable pagina);
}
