package br.com.medshare.prevalidacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RevisaoCentralRepository extends JpaRepository<RevisaoCentral, Long> {

    List<RevisaoCentral> findByDoacaoIdOrderByCriadoEmDesc(Long doacaoId);
}
