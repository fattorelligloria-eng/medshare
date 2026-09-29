package br.com.medshare.prevalidacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalisePreValidacaoRepository extends JpaRepository<AnalisePreValidacao, Long> {

    Optional<AnalisePreValidacao> findByDoacaoId(Long doacaoId);
}
