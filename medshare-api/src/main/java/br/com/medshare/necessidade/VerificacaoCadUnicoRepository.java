package br.com.medshare.necessidade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VerificacaoCadUnicoRepository extends JpaRepository<VerificacaoCadUnico, Long> {

    Optional<VerificacaoCadUnico> findFirstByUsuarioIdOrderByValidoAteDesc(Long usuarioId);

    /** UC04 A2 - consultas que pegaram o Portal fora do ar. */
    List<VerificacaoCadUnico> findByConsultaPendenteTrue();
}
