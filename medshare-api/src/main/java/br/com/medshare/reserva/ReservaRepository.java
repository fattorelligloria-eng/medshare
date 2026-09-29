package br.com.medshare.reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByCodigoRetirada(String codigoRetirada);

    Optional<Reserva> findByDoacaoIdAndStatus(Long doacaoId, StatusReserva status);

    List<Reserva> findByNecessidadeBeneficiarioIdOrderByCriadaEmDesc(Long beneficiarioId);

    /** RN04 - uma reserva ativa por principio ativo por beneficiario. */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reserva r
            WHERE r.necessidade.beneficiario.id = :beneficiarioId
              AND r.status = br.com.medshare.reserva.StatusReserva.ATIVA
              AND LOWER(TRIM(r.doacao.medicamento.principioAtivo)) = LOWER(TRIM(:principioAtivo))
            """)
    boolean existeAtivaDoPrincipioAtivo(@Param("beneficiarioId") Long beneficiarioId,
                                        @Param("principioAtivo") String principioAtivo);

    /** Reservas ativas cuja caixa ja nao alcanca a validade minima na retirada (RN02). */
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.status = br.com.medshare.reserva.StatusReserva.ATIVA
              AND r.doacao.validade < :validadeMinima
            """)
    List<Reserva> ativasComValidadeAbaixoDe(@Param("validadeMinima") java.time.LocalDate validadeMinima);

    /** Alimenta a rotina que devolve ao estoque o que ninguem retirou. */
    List<Reserva> findByStatusAndExpiraEmBefore(StatusReserva status, OffsetDateTime momento);
}
