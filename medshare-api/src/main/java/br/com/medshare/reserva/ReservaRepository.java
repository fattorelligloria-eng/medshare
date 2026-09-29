package br.com.medshare.reserva;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByCodigoRetirada(String codigoRetirada);

    Optional<Reserva> findByDoacaoIdAndStatus(Long doacaoId, StatusReserva status);

    List<Reserva> findByNecessidadeBeneficiarioIdOrderByCriadaEmDesc(Long beneficiarioId);

    /** Alimenta a rotina que devolve ao estoque o que ninguem retirou. */
    List<Reserva> findByStatusAndExpiraEmBefore(StatusReserva status, OffsetDateTime momento);
}
