package br.com.medshare.doacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DoacaoRepository extends JpaRepository<Doacao, Long> {

    Optional<Doacao> findByCodigo(String codigo);

    Page<Doacao> findByDoadorIdOrderByCriadoEmDesc(Long doadorId, Pageable pagina);

    long countByDoadorId(Long doadorId);

    long countByDoadorIdAndStatus(Long doadorId, StatusDoacao status);

    /**
     * Quanto custariam, na farmacia, as caixas que a pessoa ja doou e chegaram
     * a alguem. E o preco de tabela da CMED, nao dinheiro que mudou de mao.
     */
    @Query("""
            select coalesce(sum(d.medicamento.pmc), 0)
              from Doacao d
             where d.doador.id = :doadorId
               and d.status = br.com.medshare.doacao.StatusDoacao.ENTREGUE
            """)
    BigDecimal valorEntreguePeloDoador(@Param("doadorId") Long doadorId);

    /** UC10 passo 1 - a fila da central, de quem espera ha mais tempo para quem chegou agora. */
    Page<Doacao> findByStatusOrderByAtualizadoEmAsc(StatusDoacao status, Pageable pagina);

    /** Fila de trabalho do balcao: o que chega hoje naquela farmacia. */
    Page<Doacao> findByPontoDeColetaIdAndStatusInOrderByAtualizadoEmDesc(
            Long pontoDeColetaId, List<StatusDoacao> status, Pageable pagina);

    /** UC03 A3 - o doador chegou sem o codigo: busca pelo CPF ou telefone dele. */
    @Query("""
            SELECT d FROM Doacao d
            WHERE d.pontoDeColeta.id = :pontoId
              AND d.status IN :status
              AND (d.doador.cpf = :documento OR d.doador.telefone = :documento)
            ORDER BY d.atualizadoEm DESC
            """)
    List<Doacao> doDoadorNoPonto(@Param("pontoId") Long pontoId,
                                 @Param("status") List<StatusDoacao> status,
                                 @Param("documento") String documento);

    /**
     * UC05 - estoque de um principio ativo, do mais perto de vencer para o mais
     * longe. Qualquer apresentacao ou marca serve: quem precisa de semaglutida
     * nao liga se a caixa veio de uma farmacia ou de outra fabricante — quem
     * confere a dose com a receita e o farmaceutico (RN03).
     *
     * Caixa com oferta aberta fica de fora: ja esta prometida a alguem.
     */
    @Query("""
            SELECT d FROM Doacao d
            WHERE LOWER(TRIM(d.medicamento.principioAtivo)) = LOWER(TRIM(:principioAtivo))
              AND d.status = br.com.medshare.doacao.StatusDoacao.DISPONIVEL
              AND d.validade >= :validadeMinima
              AND NOT EXISTS (
                  SELECT o FROM Oferta o
                  WHERE o.doacao = d AND o.status = br.com.medshare.reserva.Oferta.Status.PENDENTE)
            ORDER BY d.validade ASC
            """)
    List<Doacao> disponiveisDoPrincipioAtivo(@Param("principioAtivo") String principioAtivo,
                                             @Param("validadeMinima") LocalDate validadeMinima);

    /** Caixas paradas que ja nao alcancam a validade minima (RN02). */
    @Query("""
            SELECT d FROM Doacao d
            WHERE d.status = br.com.medshare.doacao.StatusDoacao.DISPONIVEL
              AND d.validade < :validadeMinima
            """)
    List<Doacao> disponiveisVencendo(@Param("validadeMinima") LocalDate validadeMinima);

    /** Rotina de seguranca: estoque sem oferta aberta, para tentar oferecer de novo. */
    @Query("""
            SELECT d FROM Doacao d
            WHERE d.status = br.com.medshare.doacao.StatusDoacao.DISPONIVEL
              AND NOT EXISTS (
                  SELECT o FROM Oferta o
                  WHERE o.doacao = d AND o.status = br.com.medshare.reserva.Oferta.Status.PENDENTE)
            """)
    List<Doacao> disponiveisSemOferta();

    /** UC08 A2 - o que ainda esta sob a guarda de uma farmacia. */
    @Query("""
            SELECT d FROM Doacao d
            WHERE d.pontoDeColeta.id = :pontoId
              AND d.status IN (br.com.medshare.doacao.StatusDoacao.AGENDADA,
                               br.com.medshare.doacao.StatusDoacao.RECEBIDA,
                               br.com.medshare.doacao.StatusDoacao.VALIDADA,
                               br.com.medshare.doacao.StatusDoacao.DISPONIVEL,
                               br.com.medshare.doacao.StatusDoacao.RESERVADA)
            """)
    List<Doacao> sobGuardaDoPonto(@Param("pontoId") Long pontoId);

    /** UC10 A3 - casos parados na central ha mais tempo que o prazo. */
    @Query("""
            SELECT d FROM Doacao d
            WHERE d.status = br.com.medshare.doacao.StatusDoacao.EM_ANALISE_CENTRAL
              AND d.atualizadoEm < :desde AND d.atualizadoEm >= :ate
            """)
    List<Doacao> naCentralEntre(@Param("ate") OffsetDateTime ate, @Param("desde") OffsetDateTime desde);

    long countByStatus(StatusDoacao status);
}
