package br.com.medshare.farmacia;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PontoDeColetaRepository extends JpaRepository<PontoDeColeta, Long> {

    List<PontoDeColeta> findByAtivoTrueOrderByNome();

    boolean existsByCnpj(String cnpj);

    /**
     * O ponto com a linha travada ate o fim da transacao. O agendamento usa
     * isto para que dois doadores disputando a ultima vaga da mesma hora sejam
     * atendidos um de cada vez: o segundo so conta as vagas depois que o
     * primeiro gravou, e recebe "horario lotado" em vez de passar do limite.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PontoDeColeta p WHERE p.id = :id")
    Optional<PontoDeColeta> travarPorId(@Param("id") Long id);

    /**
     * Pontos de coleta ativos ordenados por distancia do endereco informado.
     * A conta e feita no banco pela funcao distancia_km (Haversine), e nao em
     * Java: trazer todas as farmacias para a memoria so para ordenar seria
     * desperdicio, e o banco ainda aproveita o indice.
     */
    @Query(value = """
            SELECT p.*, distancia_km(:latitude, :longitude, p.latitude, p.longitude) AS km
            FROM ponto_coleta p
            WHERE p.ativo = TRUE
            ORDER BY km
            LIMIT :quantidade
            """, nativeQuery = true)
    List<PontoDeColeta> maisProximosDe(@Param("latitude") double latitude,
                                       @Param("longitude") double longitude,
                                       @Param("quantidade") int quantidade);
}
