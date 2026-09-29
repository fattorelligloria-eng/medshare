package br.com.medshare.necessidade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcuradorRepository extends JpaRepository<Procurador, Long> {

    List<Procurador> findByBeneficiarioIdOrderByNome(Long beneficiarioId);

    Optional<Procurador> findByBeneficiarioIdAndCpf(Long beneficiarioId, String cpf);

    boolean existsByBeneficiarioIdAndCpf(Long beneficiarioId, String cpf);
}
