package br.com.medshare.doacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValidacaoRepository extends JpaRepository<Validacao, Long> {

    Optional<Validacao> findByDoacaoId(Long doacaoId);
}
