package br.com.medshare.necessidade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificacaoCadUnicoRepository extends JpaRepository<VerificacaoCadUnico, Long> {

    Optional<VerificacaoCadUnico> findFirstByUsuarioIdOrderByValidoAteDesc(Long usuarioId);
}
