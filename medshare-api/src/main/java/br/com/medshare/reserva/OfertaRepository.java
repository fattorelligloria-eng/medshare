package br.com.medshare.reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    Optional<Oferta> findFirstByDoacaoIdAndStatus(Long doacaoId, Oferta.Status status);

    boolean existsByNecessidadeIdAndStatus(Long necessidadeId, Oferta.Status status);

    /** Quem ja recusou ou deixou vencer esta caixa nao a recebe de novo. */
    @Query("""
            SELECT o.necessidade.id FROM Oferta o
            WHERE o.doacao.id = :doacaoId
              AND o.status IN (br.com.medshare.reserva.Oferta.Status.RECUSADA,
                               br.com.medshare.reserva.Oferta.Status.EXPIRADA)
            """)
    List<Long> necessidadesQueJaDispensaram(@Param("doacaoId") Long doacaoId);

    /** Caixas que este pedido ja recusou ou deixou vencer. */
    @Query("""
            SELECT o.doacao.id FROM Oferta o
            WHERE o.necessidade.id = :necessidadeId
              AND o.status IN (br.com.medshare.reserva.Oferta.Status.RECUSADA,
                               br.com.medshare.reserva.Oferta.Status.EXPIRADA)
            """)
    List<Long> doacoesDispensadasPor(@Param("necessidadeId") Long necessidadeId);

    @Query("""
            SELECT o FROM Oferta o
            WHERE o.necessidade.beneficiario.id = :beneficiarioId
            ORDER BY o.criadaEm DESC
            """)
    List<Oferta> doBeneficiario(@Param("beneficiarioId") Long beneficiarioId);

    List<Oferta> findByStatusAndExpiraEmBefore(Oferta.Status status, OffsetDateTime momento);
}
