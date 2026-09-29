package br.com.medshare.doacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    /** Uma doacao pode ter ate dois agendamentos (UC02 A2); vale o mais recente. */
    Optional<Agendamento> findFirstByDoacaoIdOrderByCriadoEmDesc(Long doacaoId);

    long countByDoacaoId(Long doacaoId);

    /** UC03 passo 1 - o farmaceutico le o codigo de entrega que o doador mostra. */
    Optional<Agendamento> findFirstByCodigoEntregaOrderByCriadoEmDesc(String codigoEntrega);

    /** Vagas ja tomadas numa farmacia, numa faixa de horario. */
    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.pontoDeColeta.id = :pontoId
              AND a.dataHora >= :inicio AND a.dataHora < :fim
              AND a.doacao.status = br.com.medshare.doacao.StatusDoacao.AGENDADA
            """)
    List<Agendamento> ocupadosNoPeriodo(@Param("pontoId") Long pontoId,
                                        @Param("inicio") OffsetDateTime inicio,
                                        @Param("fim") OffsetDateTime fim);

    /** UC02 passo 3 - lembrete da vespera. */
    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.lembreteEnviado = FALSE
              AND a.dataHora >= :inicio AND a.dataHora < :fim
              AND a.doacao.status = br.com.medshare.doacao.StatusDoacao.AGENDADA
            """)
    List<Agendamento> semLembreteEntre(@Param("inicio") OffsetDateTime inicio,
                                       @Param("fim") OffsetDateTime fim);

    /** UC02 A2 - agendamentos vencidos ha mais de 7 dias sem comparecimento. */
    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.dataHora < :limite
              AND a.compareceu IS NULL
              AND a.doacao.status = br.com.medshare.doacao.StatusDoacao.AGENDADA
            """)
    List<Agendamento> faltasAntesDe(@Param("limite") OffsetDateTime limite);
}
