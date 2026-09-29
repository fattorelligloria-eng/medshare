package br.com.medshare.necessidade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NecessidadeRepository extends JpaRepository<Necessidade, Long> {

    List<Necessidade> findByBeneficiarioIdAndAtivaTrue(Long beneficiarioId);

    Optional<Necessidade> findByBeneficiarioIdAndMedicamentoIdAndAtivaTrue(
            Long beneficiarioId, Long medicamentoId);

    /**
     * Fila de espera de um medicamento, por ordem de chegada.
     * Quem pediu primeiro e atendido primeiro: e o criterio mais simples de
     * explicar para quem esta esperando, e o unico que nao exige o sistema
     * julgar quem precisa mais.
     */
    @Query("""
            SELECT n FROM Necessidade n
            WHERE n.medicamento.id = :medicamentoId
              AND n.ativa = TRUE
            ORDER BY n.criadaEm ASC
            """)
    List<Necessidade> filaDeEsperaDoMedicamento(@Param("medicamentoId") Long medicamentoId);
}
