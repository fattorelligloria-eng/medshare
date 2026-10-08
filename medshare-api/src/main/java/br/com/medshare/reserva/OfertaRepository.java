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

    /**
     * Quem ja recusou, deixou vencer ou ja aceitou esta caixa nao a recebe de
     * novo. ACEITA entra porque, se a caixa voltou ao estoque, a reserva dessa
     * pessoa caiu (cancelou, nao retirou ou teve a entrega negada) — sem isso a
     * mesma caixa voltava para ela na hora, primeira da fila, e nunca chegava a
     * proxima pessoa.
     */
    @Query("""
            SELECT o.necessidade.id FROM Oferta o
            WHERE o.doacao.id = :doacaoId
              AND o.status IN (br.com.medshare.reserva.Oferta.Status.RECUSADA,
                               br.com.medshare.reserva.Oferta.Status.EXPIRADA,
                               br.com.medshare.reserva.Oferta.Status.ACEITA)
            """)
    List<Long> necessidadesQueJaDispensaram(@Param("doacaoId") Long doacaoId);

    /** Caixas que este pedido ja recusou, deixou vencer ou aceitou e perdeu. */
    @Query("""
            SELECT o.doacao.id FROM Oferta o
            WHERE o.necessidade.id = :necessidadeId
              AND o.status IN (br.com.medshare.reserva.Oferta.Status.RECUSADA,
                               br.com.medshare.reserva.Oferta.Status.EXPIRADA,
                               br.com.medshare.reserva.Oferta.Status.ACEITA)
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
