package br.com.medshare.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    @org.springframework.data.jpa.repository.Query("""
            SELECT DISTINCT u FROM Usuario u JOIN u.papeis p
            WHERE p = :papel AND u.ativo = TRUE
            """)
    java.util.List<Usuario> ativosComPapel(
            @org.springframework.data.repository.query.Param("papel") Papel papel);
}
