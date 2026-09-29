package br.com.medshare.necessidade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceitaRepository extends JpaRepository<Receita, Long> {

    Optional<Receita> findByNecessidadeId(Long necessidadeId);
}
