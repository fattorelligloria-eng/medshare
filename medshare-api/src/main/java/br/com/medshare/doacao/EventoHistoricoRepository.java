package br.com.medshare.doacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoHistoricoRepository extends JpaRepository<EventoHistorico, Long> {

    List<EventoHistorico> findByDoacaoIdOrderByOcorridoEmAscIdAsc(Long doacaoId);
}
