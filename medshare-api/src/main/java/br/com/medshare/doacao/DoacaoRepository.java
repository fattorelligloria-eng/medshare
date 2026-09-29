package br.com.medshare.doacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoacaoRepository extends JpaRepository<Doacao, Long> {

    Optional<Doacao> findByCodigo(String codigo);

    Page<Doacao> findByDoadorIdOrderByCriadoEmDesc(Long doadorId, Pageable pagina);

    Page<Doacao> findByStatusOrderByCriadoEmDesc(StatusDoacao status, Pageable pagina);

    /** Fila de trabalho do balcao: o que chega hoje naquela farmacia. */
    Page<Doacao> findByPontoDeColetaIdAndStatusInOrderByAtualizadoEmDesc(
            Long pontoDeColetaId, List<StatusDoacao> status, Pageable pagina);

    /**
     * Estoque de um medicamento, do mais perto de vencer para o mais longe.
     * Essa ordem nao e detalhe: ela faz a caixa que vence antes sair primeiro,
     * o que reduz desperdicio.
     */
    @Query("""
            SELECT d FROM Doacao d
            WHERE d.medicamento.id = :medicamentoId
              AND d.status = br.com.medshare.doacao.StatusDoacao.DISPONIVEL
              AND d.validade >= :validadeMinima
            ORDER BY d.validade ASC
            """)
    List<Doacao> disponiveisDoMedicamento(@Param("medicamentoId") Long medicamentoId,
                                          @Param("validadeMinima") LocalDate validadeMinima);

    /** Caixas paradas que ja nao alcancam a validade minima (RN02). */
    @Query("""
            SELECT d FROM Doacao d
            WHERE d.status = br.com.medshare.doacao.StatusDoacao.DISPONIVEL
              AND d.validade < :validadeMinima
            """)
    List<Doacao> disponiveisVencendo(@Param("validadeMinima") LocalDate validadeMinima);

    long countByStatus(StatusDoacao status);
}
