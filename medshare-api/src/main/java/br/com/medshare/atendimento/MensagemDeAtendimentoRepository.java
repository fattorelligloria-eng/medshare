package br.com.medshare.atendimento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensagemDeAtendimentoRepository extends JpaRepository<MensagemDeAtendimento, Long> {

    List<MensagemDeAtendimento> findTop20ByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    long countByUsuarioIdAndRespostaIsNull(Long usuarioId);

    /** A fila da central: quem espera ha mais tempo primeiro. Traz o nome junto, sem uma consulta por linha. */
    @EntityGraph(attributePaths = "usuario")
    Page<MensagemDeAtendimento> findByRespostaIsNullOrderByCriadoEmAsc(Pageable pagina);
}
