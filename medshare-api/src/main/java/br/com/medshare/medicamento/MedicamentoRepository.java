package br.com.medshare.medicamento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    Optional<Medicamento> findByRegistroAnvisa(String registroAnvisa);

    Optional<Medicamento> findByEan(String ean);

    /** Busca do app: o usuario digita parte do nome ou do principio ativo. */
    @Query("""
            SELECT m FROM Medicamento m
            WHERE m.altoCusto = TRUE
              AND (LOWER(m.nomeComercial)  LIKE LOWER(CONCAT('%', :termo, '%'))
                OR LOWER(m.principioAtivo) LIKE LOWER(CONCAT('%', :termo, '%')))
            ORDER BY m.nomeComercial
            """)
    Page<Medicamento> buscarDeAltoCusto(@Param("termo") String termo, Pageable pagina);
}
