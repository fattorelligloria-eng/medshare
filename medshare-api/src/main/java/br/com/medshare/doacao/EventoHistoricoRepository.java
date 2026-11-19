package br.com.medshare.doacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.time.OffsetDateTime;

import java.util.List;

public interface EventoHistoricoRepository extends JpaRepository<EventoHistorico, Long> {

    List<EventoHistorico> findByDoacaoIdOrderByOcorridoEmAscIdAsc(Long doacaoId);

    /**
     * Quando cada caixa de um principio ativo entrou no estoque, da mais
     * recente para a mais antiga.
     *
     * Sai do historico e nao do estado atual da doacao de proposito: o
     * historico e imutavel (RN06), entao ele sabe o momento exato em que a
     * caixa ficou disponivel mesmo que ela ja tenha sido entregue depois.
     * Olhar o status atual so acharia as que ainda estao na prateleira.
     */
    @Query("""
            SELECT e.ocorridoEm FROM EventoHistorico e
            WHERE e.tipo = br.com.medshare.doacao.TipoEvento.DISPONIBILIZACAO
              AND LOWER(TRIM(e.doacao.medicamento.principioAtivo)) = LOWER(TRIM(:principioAtivo))
              AND e.ocorridoEm >= :desde
            ORDER BY e.ocorridoEm DESC
            """)
    List<OffsetDateTime> disponibilizacoesDesde(@Param("principioAtivo") String principioAtivo,
                                                @Param("desde") OffsetDateTime desde);
}
