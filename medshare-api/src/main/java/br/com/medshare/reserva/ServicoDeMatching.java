package br.com.medshare.reserva;

import br.com.medshare.comum.PropriedadesDoMedShare;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.doacao.DoacaoRepository;
import br.com.medshare.necessidade.Necessidade;
import br.com.medshare.usuario.Endereco;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Escolhe qual caixa disponivel atende uma necessidade.
 *
 * O criterio tem duas camadas, nesta ordem:
 *
 *  1. Vence antes, sai antes. Medicamento parado na prateleira vira lixo
 *     hospitalar; tirar primeiro o que esta mais perto do vencimento e o que
 *     reduz desperdicio na rede inteira.
 *
 *  2. Entre caixas com validade parecida, a farmacia mais perto do beneficiario.
 *     Uma pessoa que depende de transporte publico nao atravessa a Grande Sao
 *     Paulo para economizar duas semanas de prazo.
 *
 * "Validade parecida" e a janela definida abaixo: sem ela, uma diferenca de um
 * dia no vencimento mandaria alguem de Itaquaquecetuba buscar remedio em Cotia.
 */
@Service
public class ServicoDeMatching {

    /** Caixas que vencem dentro desta janela sao tratadas como equivalentes. */
    private static final int JANELA_DE_EQUIVALENCIA_EM_DIAS = 30;

    private final DoacaoRepository doacoes;
    private final PropriedadesDoMedShare propriedades;

    public ServicoDeMatching(DoacaoRepository doacoes, PropriedadesDoMedShare propriedades) {
        this.doacoes = doacoes;
        this.propriedades = propriedades;
    }

    public Optional<Doacao> melhorDoacaoPara(Necessidade necessidade) {
        LocalDate validadeMinima = LocalDate.now()
                .plusDays(propriedades.doacao().diasMinimosDeValidade());   // RN02

        List<Doacao> candidatas = doacoes.disponiveisDoMedicamento(
                necessidade.getMedicamento().getId(), validadeMinima);

        if (candidatas.isEmpty()) {
            return Optional.empty();
        }

        LocalDate limiteDaJanela = candidatas.get(0).getValidade()
                .plusDays(JANELA_DE_EQUIVALENCIA_EM_DIAS);

        return candidatas.stream()
                .filter(doacao -> !doacao.getValidade().isAfter(limiteDaJanela))
                .min(Comparator.comparingDouble(
                        doacao -> distanciaAte(doacao, necessidade)));
    }

    /**
     * Distancia em quilometros entre o beneficiario e a farmacia onde a caixa
     * esta. Sem coordenadas de um dos lados, devolve um valor alto para que a
     * opcao so seja escolhida se nao houver outra — nunca descarta a caixa.
     */
    private double distanciaAte(Doacao doacao, Necessidade necessidade) {
        Endereco deQuemPrecisa = necessidade.getBeneficiario().getEndereco();
        if (doacao.getPontoDeColeta() == null || !deQuemPrecisa.temCoordenadas()) {
            return Double.MAX_VALUE;
        }
        Endereco daFarmacia = doacao.getPontoDeColeta().getEndereco();
        if (!daFarmacia.temCoordenadas()) {
            return Double.MAX_VALUE;
        }
        return distanciaEmKm(
                deQuemPrecisa.getLatitude(), deQuemPrecisa.getLongitude(),
                daFarmacia.getLatitude(), daFarmacia.getLongitude());
    }

    /** Formula de Haversine — a mesma que a funcao distancia_km do banco. */
    private double distanciaEmKm(double latitude1, double longitude1,
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
