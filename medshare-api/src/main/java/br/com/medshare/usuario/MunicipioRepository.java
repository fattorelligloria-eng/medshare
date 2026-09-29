package br.com.medshare.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MunicipioRepository extends JpaRepository<Municipio, Short> {

    Optional<Municipio> findByNomeIgnoreCase(String nome);

    Optional<Municipio> findByCodigoIbge(String codigoIbge);
}
