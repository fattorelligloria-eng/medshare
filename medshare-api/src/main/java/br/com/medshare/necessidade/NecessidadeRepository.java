package br.com.medshare.necessidade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface NecessidadeRepository extends JpaRepository<Necessidade, Long> {

    List<Necessidade> findByBeneficiarioIdAndAtivaTrue(Long beneficiarioId);

    /** RN04 / UC04 A4 - um pedido ativo por principio ativo. */
    @Query("""
            SELECT n FROM Necessidade n
            WHERE n.beneficiario.id = :beneficiarioId
              AND n.ativa = TRUE
              AND LOWER(TRIM(n.medicamento.principioAtivo)) = LOWER(TRIM(:principioAtivo))
            """)
    Optional<Necessidade> ativaDoPrincipioAtivo(@Param("beneficiarioId") Long beneficiarioId,
                                                @Param("principioAtivo") String principioAtivo);

    /**
     * UC05 - a fila de um principio ativo: quem perdeu uma caixa por validade
     * primeiro (UC07 A2), depois por ordem de chegada. So entra quem esta fora
     * de revisao e sem oferta aberta; a distancia e a receita sao conferidas
     * depois, no matching.
     */
    @Query("""
            SELECT n FROM Necessidade n
            WHERE LOWER(TRIM(n.medicamento.principioAtivo)) = LOWER(TRIM(:principioAtivo))
              AND n.ativa = TRUE
              AND n.emRevisao = FALSE
              AND NOT EXISTS (
                  SELECT o FROM Oferta o
                  WHERE o.necessidade = n AND o.status = br.com.medshare.reserva.Oferta.Status.PENDENTE)
            ORDER BY n.prioridade DESC, n.criadaEm ASC
            """)
    List<Necessidade> filaDoPrincipioAtivo(@Param("principioAtivo") String principioAtivo);

    /**
     * Quanta gente espera pelo mesmo principio ativo.
     *
     * Mesmo recorte da fila de verdade — ativa e fora de revisao — para o
     * numero na tela nao divergir do numero que o matching enxerga.
     */
    @Query("""
            SELECT COUNT(n) FROM Necessidade n
            WHERE LOWER(TRIM(n.medicamento.principioAtivo)) = LOWER(TRIM(:principioAtivo))
              AND n.ativa = TRUE
              AND n.emRevisao = FALSE
            """)
    long contarNaFila(@Param("principioAtivo") String principioAtivo);

    /** Quantos entraram na fila depois de uma data. */
    @Query("""
            SELECT COUNT(n) FROM Necessidade n
            WHERE LOWER(TRIM(n.medicamento.principioAtivo)) = LOWER(TRIM(:principioAtivo))
              AND n.ativa = TRUE
              AND n.emRevisao = FALSE
              AND n.criadaEm > :depoisDe
            """)
    long contarEntrantesApos(@Param("principioAtivo") String principioAtivo,
                             @Param("depoisDe") OffsetDateTime depoisDe);
}
