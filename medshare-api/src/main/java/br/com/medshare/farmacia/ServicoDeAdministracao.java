package br.com.medshare.farmacia;

import br.com.medshare.comum.RecursoNaoEncontrado;
import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.doacao.DoacaoRepository;
import br.com.medshare.doacao.StatusDoacao;
import br.com.medshare.reserva.ServicoDeOferta;
import br.com.medshare.usuario.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UC08 - o administrador cuida da rede de farmacias parceiras.
 *
 * Uma farmacia nasce INATIVA e so aparece para doacao e retirada quando tem um
 * farmaceutico responsavel com CRF. Para ser desativada, nao pode ter caixa sob
 * a guarda dela: as disponiveis vao para outra farmacia primeiro (A2).
 */
@Service
public class ServicoDeAdministracao {

    private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
            "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC",
            "SP", "SE", "TO");

    private final PontoDeColetaRepository pontos;
    private final FarmaceuticoRepository farmaceuticos;
    private final UsuarioRepository usuarios;
    private final DoacaoRepository doacoes;
    private final ServicoDeEndereco enderecos;
    private final ServicoDeOferta ofertas;

    public ServicoDeAdministracao(PontoDeColetaRepository pontos, FarmaceuticoRepository farmaceuticos,
                                  UsuarioRepository usuarios, DoacaoRepository doacoes,
                                  ServicoDeEndereco enderecos, ServicoDeOferta ofertas) {
        this.pontos = pontos;
        this.farmaceuticos = farmaceuticos;
        this.usuarios = usuarios;
        this.doacoes = doacoes;
        this.enderecos = enderecos;
        this.ofertas = ofertas;
    }

    public record DadosDoPonto(String nome, String cnpj, String cep, String logradouro, String numero,
                               String complemento, String bairro, Short municipioId,
                               Double latitude, Double longitude,
                               Set<DayOfWeek> dias, LocalTime abreAs, LocalTime fechaAs, int vagasPorHora) { }

    @Transactional(readOnly = true)
    public List<PontoDeColeta> todosOsPontos() {
        return pontos.findAll(org.springframework.data.domain.Sort.by("nome"));
    }

    /** UC08 passo 1 - cadastra a farmacia, ainda inativa. */
    @Transactional
    public PontoDeColeta cadastrarPonto(DadosDoPonto dados) {
        String cnpj = dados.cnpj().replaceAll("\\D", "");
        if (!CnpjValido.verificar(cnpj)) {
            throw new RegraDeNegocioViolada("PONTO", "CNPJ inválido: confira os dígitos");
        }
        if (pontos.existsByCnpj(cnpj)) {
            throw new RegraDeNegocioViolada("PONTO", "Já existe uma farmácia com este CNPJ");
        }
        Endereco endereco = enderecos.montar(dados.cep(), dados.logradouro(), dados.numero(),
                dados.complemento(), dados.bairro(), dados.municipioId(), dados.latitude(), dados.longitude());
        if (!endereco.temCoordenadas()) {
            // A farmacia e o ponto de referencia da distancia (UC02, UC05); sem
            // coordenada ela nunca apareceria como "mais proxima".
            throw new RegraDeNegocioViolada("PONTO",
                    "Não consegui localizar este endereço no mapa. Informe latitude e longitude.");
        }
        PontoDeColeta ponto = new PontoDeColeta(dados.nome().trim(), cnpj, endereco,
                dados.dias(), dados.abreAs(), dados.fechaAs(), dados.vagasPorHora());
        ponto.desativar();   // so ativa com farmaceutico responsavel (UC08 passo 2)
        return pontos.save(ponto);
    }

    @Transactional
    public PontoDeColeta alterarHorario(Long pontoId, Set<DayOfWeek> dias, LocalTime abre,
                                        LocalTime fecha, int vagas) {
        PontoDeColeta ponto = buscarPonto(pontoId);
        ponto.definirHorario(dias, abre, fecha, vagas);
        return ponto;
    }

    /**
     * UC08 passo 2 - vincula o farmaceutico responsavel, confere o CRF e ativa
     * a farmacia. A pessoa precisa ja ter conta no MedShare; ganha o papel de
     * farmaceutico aqui (ninguem se cadastra sozinho como farmaceutico).
     *
     * A1: CRF fora do formato e recusado e a farmacia continua inativa. Nao ha
     * API publica do Conselho Federal de Farmacia; a conferencia do numero no
     * site do CRF e feita pelo administrador antes deste passo.
     */
    @Transactional
    public Farmaceutico vincularFarmaceutico(Long pontoId, String email, String crf, String ufCrf) {
        PontoDeColeta ponto = buscarPonto(pontoId);
        String numero = crf == null ? "" : crf.replaceAll("\\D", "");
        String uf = ufCrf == null ? "" : ufCrf.trim().toUpperCase();
        if (numero.isEmpty() || numero.length() > 8) {
            throw new RegraDeNegocioViolada("CRF", "CRF inválido: informe só os números do registro");
        }
        if (!UFS.contains(uf)) {
            throw new RegraDeNegocioViolada("CRF", "UF do CRF inválida");
        }
        farmaceuticos.findByCrf(numero)
                .filter(outro -> !outro.getUsuario().getEmail().equalsIgnoreCase(email.trim()))
                .ifPresent(outro -> {
                    throw new RegraDeNegocioViolada("CRF", "Este CRF já está vinculado a outra pessoa");
                });

        Usuario usuario = usuarios.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RegraDeNegocioViolada("CRF",
                        "Não há conta com o e-mail %s. A pessoa precisa se cadastrar no MedShare antes."
                                .formatted(email)));
        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioViolada("CRF", "A conta de %s está desativada".formatted(email));
        }

        usuario.adicionarPapel(Papel.FARMACEUTICO);
        Farmaceutico farmaceutico = farmaceuticos.findById(usuario.getId())
                .map(existente -> {
                    existente.passarAAtuarEm(ponto, numero, uf);
                    return existente;
                })
                .orElseGet(() -> farmaceuticos.save(new Farmaceutico(usuario, numero, uf, ponto)));
        ponto.ativar();
        return farmaceutico;
    }

    @Transactional(readOnly = true)
    public List<Farmaceutico> farmaceuticosDo(Long pontoId) {
        return farmaceuticos.findByPontoDeColetaId(pontoId);
    }

    /** UC08 A2 - leva o estoque disponivel para outra farmacia antes de desativar. */
    @Transactional
    public int transferirEstoque(Long origemId, Long destinoId, Usuario admin) {
        if (origemId.equals(destinoId)) {
            throw new RegraDeNegocioViolada("PONTO", "Escolha uma farmácia de destino diferente da origem");
        }
        buscarPonto(origemId);
        PontoDeColeta destino = buscarPonto(destinoId);
        if (!destino.isAtivo()) {
            throw new RegraDeNegocioViolada("PONTO", "A farmácia de destino precisa estar ativa");
        }
        List<Doacao> disponiveis = doacoes.sobGuardaDoPonto(origemId).stream()
                .filter(Doacao::estaDisponivel)
                .toList();
        for (Doacao doacao : disponiveis) {
            ofertas.cancelarPendenteDa(doacao);
            doacao.transferirPara(destino, admin);
            ofertas.ofertarDoacao(doacao);
        }
        return disponiveis.size();
    }

    /** UC08 A2 - so desativa sem nenhuma caixa sob a guarda da farmacia. */
    @Transactional
    public PontoDeColeta desativar(Long pontoId) {
        PontoDeColeta ponto = buscarPonto(pontoId);
        Map<StatusDoacao, Long> sobGuarda = doacoes.sobGuardaDoPonto(pontoId).stream()
                .collect(Collectors.groupingBy(Doacao::getStatus, Collectors.counting()));
        if (!sobGuarda.isEmpty()) {
            String resumo = sobGuarda.entrySet().stream()
                    .map(e -> "%d %s".formatted(e.getValue(), e.getKey().name().toLowerCase()))
                    .collect(Collectors.joining(", "));
            throw new RegraDeNegocioViolada("PONTO",
                    "%s ainda tem caixas sob sua guarda (%s). Transfira as disponíveis para outra farmácia "
                            .formatted(ponto.getNome(), resumo)
                            + "e aguarde as agendadas e reservadas terminarem antes de desativar.");
        }
        ponto.desativar();
        return ponto;
    }

    private PontoDeColeta buscarPonto(Long id) {
        return pontos.findById(id).orElseThrow(() -> new RecursoNaoEncontrado("Ponto de coleta", id));
    }

    /** Digitos verificadores do CNPJ (modulo 11). */
    static final class CnpjValido {
        private static final int[] PESOS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        private static final int[] PESOS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        static boolean verificar(String cnpj) {
            if (cnpj == null || !cnpj.matches("\\d{14}") || cnpj.chars().distinct().count() == 1) {
                return false;
            }
            return digito(cnpj, PESOS_1) == cnpj.charAt(12) - '0'
                    && digito(cnpj, PESOS_2) == cnpj.charAt(13) - '0';
        }

        private static int digito(String cnpj, int[] pesos) {
            int soma = 0;
            for (int i = 0; i < pesos.length; i++) {
                soma += (cnpj.charAt(i) - '0') * pesos[i];
            }
            int resto = soma % 11;
            return resto < 2 ? 0 : 11 - resto;
        }
    }
}
