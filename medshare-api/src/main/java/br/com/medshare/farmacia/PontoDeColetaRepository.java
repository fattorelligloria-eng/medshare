package br.com.medshare.farmacia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PontoDeColetaRepository extends JpaRepository<PontoDeColeta, Long> {

    List<PontoDeColeta> findByAtivoTrueOrderByNome();

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
