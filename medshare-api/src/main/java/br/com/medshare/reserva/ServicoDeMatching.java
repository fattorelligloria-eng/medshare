package br.com.medshare.reserva;

import br.com.medshare.comum.PropriedadesDoMedShare;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.doacao.DoacaoRepository;
import br.com.medshare.farmacia.PontoDeColeta;
import br.com.medshare.necessidade.Necessidade;
import br.com.medshare.usuario.Endereco;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Casa caixas disponiveis com pedidos (UC05).
 *
 * O casamento e pelo PRINCIPIO ATIVO, e nao pela apresentacao: o catalogo da
 * CMED tem dezenas de apresentacoes do mesmo remedio (dose, tamanho, marca), e
 * exigir a mesma linha faria quase nada casar. A dose certa e conferida pelo
 * farmaceutico contra a receita no balcao (RN03).
 *
 * Nos dois sentidos o criterio e o mesmo:
 *
 *  - Caixa para um pedido: vence antes, sai antes (menos desperdicio); entre
 *    caixas de validade parecida, a farmacia mais perto de quem precisa.
 *
 *  - Pedido para uma caixa: quem perdeu uma caixa por validade primeiro
 *    (UC07 A2); depois quem esta mais perto da farmacia, em faixas de
 *    distancia; dentro da mesma faixa, quem pediu antes.
 */
@Service
public class ServicoDeMatching {

    /** Caixas que vencem dentro desta janela sao tratadas como equivalentes. */
    private static final int JANELA_DE_EQUIVALENCIA_EM_DIAS = 30;

    /**
     * Distancias ate aqui contam como "igualmente perto": sem a faixa, 300
     * metros a menos passariam alguem que espera ha meses.
     */
    private static final double FAIXA_DE_DISTANCIA_EM_KM = 10;

    private final DoacaoRepository doacoes;
    private final PropriedadesDoMedShare propriedades;

    public ServicoDeMatching(DoacaoRepository doacoes, PropriedadesDoMedShare propriedades) {
        this.doacoes = doacoes;
        this.propriedades = propriedades;
    }

    /** A melhor caixa disponivel para este pedido, pulando as que ele ja dispensou. */
    public Optional<Doacao> melhorDoacaoPara(Necessidade necessidade, Collection<Long> doacoesDispensadas) {
        LocalDate validadeMinima = LocalDate.now()
                .plusDays(propriedades.doacao().diasMinimosDeValidade());   // RN02

        List<Doacao> candidatas = doacoes.disponiveisDoPrincipioAtivo(
                        necessidade.getMedicamento().getPrincipioAtivo(), validadeMinima).stream()
                .filter(d -> d.getId() == null || !doacoesDispensadas.contains(d.getId()))
                .toList();

        if (candidatas.isEmpty()) {
            return Optional.empty();
        }

        LocalDate limiteDaJanela = candidatas.get(0).getValidade()
                .plusDays(JANELA_DE_EQUIVALENCIA_EM_DIAS);

        return candidatas.stream()
                .filter(doacao -> !doacao.getValidade().isAfter(limiteDaJanela))
                .min(Comparator.comparingDouble(
                        doacao -> distanciaAte(doacao.getPontoDeColeta(), necessidade)));
    }

    public Optional<Doacao> melhorDoacaoPara(Necessidade necessidade) {
        return melhorDoacaoPara(necessidade, List.of());
    }

    /**
     * O melhor pedido da fila para esta caixa. A fila chega ja ordenada por
     * prioridade e antiguidade; {@code apta} filtra o que depende de outras
     * tabelas (CadUnico vigente, reserva ativa - RN04, RN08).
     */
    public Optional<Necessidade> melhorNecessidadePara(Doacao doacao, List<Necessidade> fila,
                                                       Predicate<Necessidade> apta) {
        return fila.stream()
                .filter(Necessidade::aptaParaOferta)
                .filter(apta)
                .min(Comparator
                        .comparing((Necessidade n) -> !n.isPrioridade())
                        .thenComparingDouble(n -> faixa(distanciaAte(doacao.getPontoDeColeta(), n)))
                        .thenComparing(Necessidade::getCriadaEm));
    }

    private static double faixa(double km) {
        return km == Double.MAX_VALUE ? Double.MAX_VALUE : Math.floor(km / FAIXA_DE_DISTANCIA_EM_KM);
    }

    /**
     * Distancia em quilometros entre o beneficiario e a farmacia. Sem
     * coordenadas de um dos lados, devolve um valor alto para que a opcao so
     * seja escolhida se nao houver outra — nunca descarta ninguem.
     */
    private double distanciaAte(PontoDeColeta ponto, Necessidade necessidade) {
        Endereco deQuemPrecisa = necessidade.getBeneficiario().getEndereco();
        if (ponto == null || deQuemPrecisa == null || !deQuemPrecisa.temCoordenadas()) {
            return Double.MAX_VALUE;
        }
        Endereco daFarmacia = ponto.getEndereco();
        if (!daFarmacia.temCoordenadas()) {
            return Double.MAX_VALUE;
        }
        return distanciaEmKm(
                deQuemPrecisa.getLatitude(), deQuemPrecisa.getLongitude(),
                daFarmacia.getLatitude(), daFarmacia.getLongitude());
    }

    /** Formula de Haversine — a mesma que a funcao distancia_km do banco. */
    static double distanciaEmKm(double latitude1, double longitude1,
                                double latitude2, double longitude2) {
        final double raioDaTerraEmKm = 6371;
        double deltaLatitude = Math.toRadians(latitude2 - latitude1);
        double deltaLongitude = Math.toRadians(longitude2 - longitude1);

        double a = Math.pow(Math.sin(deltaLatitude / 2), 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.pow(Math.sin(deltaLongitude / 2), 2);

        return raioDaTerraEmKm * 2 * Math.asin(Math.sqrt(a));
    }
}
