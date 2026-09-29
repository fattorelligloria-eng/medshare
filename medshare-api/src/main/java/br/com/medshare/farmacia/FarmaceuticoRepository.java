package br.com.medshare.farmacia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FarmaceuticoRepository extends JpaRepository<Farmaceutico, Long> {

    Optional<Farmaceutico> findByCrf(String crf);

    List<Farmaceutico> findByPontoDeColetaId(Long pontoDeColetaId);
}
