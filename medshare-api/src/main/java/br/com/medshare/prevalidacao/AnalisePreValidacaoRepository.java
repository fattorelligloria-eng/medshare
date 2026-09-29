package br.com.medshare.prevalidacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalisePreValidacaoRepository extends JpaRepository<AnalisePreValidacao, Long> {

    /** A doacao pode ter varias analises (nova foto - UC10 A2); vale a mais recente. */
    Optional<AnalisePreValidacao> findFirstByDoacaoIdOrderByCriadoEmDesc(Long doacaoId);
}
