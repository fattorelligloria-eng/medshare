package br.com.medshare.necessidade;

import br.com.medshare.comum.*;
import br.com.medshare.integracao.ConsultaDeCadUnico;
import br.com.medshare.medicamento.Medicamento;
import br.com.medshare.medicamento.MedicamentoRepository;
import br.com.medshare.notificacao.ServicoDeNotificacao;
import br.com.medshare.notificacao.TipoNotificacao;
import br.com.medshare.reserva.ServicoDeOferta;
import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * O lado de quem precisa (UC04): NIS, pedido, receita e procuradores.
 */
@Service
public class ServicoDeNecessidade {

    private static final Logger log = LoggerFactory.getLogger(ServicoDeNecessidade.class);

    private final NecessidadeRepository necessidades;
    private final ReceitaRepository receitas;
    private final VerificacaoCadUnicoRepository verificacoes;
    private final ProcuradorRepository procuradores;
    private final MedicamentoRepository medicamentos;
    private final ConsultaDeCadUnico consultaCadUnico;
    private final ServicoDeNotificacao notificacoes;
    private final ServicoDeOferta ofertas;
    private final PropriedadesDoMedShare propriedades;

    public ServicoDeNecessidade(NecessidadeRepository necessidades, ReceitaRepository receitas,
                                VerificacaoCadUnicoRepository verificacoes,
                                ProcuradorRepository procuradores,
                                MedicamentoRepository medicamentos,
                                ConsultaDeCadUnico consultaCadUnico,
                                ServicoDeNotificacao notificacoes,
                                ServicoDeOferta ofertas,
                                PropriedadesDoMedShare propriedades) {
        this.necessidades = necessidades;
        this.receitas = receitas;
        this.verificacoes = verificacoes;
        this.procuradores = procuradores;
        this.medicamentos = medicamentos;
        this.consultaCadUnico = consultaCadUnico;
        this.notificacoes = notificacoes;
        this.ofertas = ofertas;
        this.propriedades = propriedades;
    }

    // --- RN08: CadUnico ------------------------------------------------------

    /**
     * RN08 - verifica o NIS e guarda o resultado por 12 meses.
     *
     * Formato invalido e recusado na hora (a pessoa corrige). "Nao encontrado"
     * vai para conferencia humana: a consulta oficial so enxerga quem recebe
     * Bolsa Familia, e ha quem esteja no CadUnico sem receber. Portal fora do
     * ar (UC04 A2) fica pendente e uma rotina tenta de novo.
     */
    @Transactional
    public ResultadoDaVerificacao verificarCadUnico(String nis, Usuario beneficiario) {
        ConsultaDeCadUnico.ResultadoDaConsulta resultado = consultaCadUnico.consultar(nis);

        if (!resultado.formatoValido()) {
            throw new RegraDeNegocioViolada("RN08", resultado.observacao());
        }

        VerificacaoCadUnico verificacao = new VerificacaoCadUnico(
                beneficiario, nis, resultado.confirmado(),
                propriedades.cadunico().mesesDeValidade(), consultaCadUnico.fonte());
        if (resultado.indisponivel()) {
            verificacao.marcarConsultaPendente();
        }
        verificacoes.save(verificacao);

        if (resultado.confirmado()) {
            ofertarParaPedidosDe(beneficiario);
        }
        String observacao = resultado.precisaDeAnaliseHumana()
                ? resultado.observacao() + ". Nossa equipe confere à mão em até 48 horas. "
                        + "Se você não estiver no CadÚnico, procure o CRAS do seu bairro para se inscrever."
                : resultado.observacao();
        return new ResultadoDaVerificacao(verificacao, resultado.precisaDeAnaliseHumana(), observacao);
    }

    /** O que a tela precisa saber depois de uma verificação. */
    public record ResultadoDaVerificacao(
            VerificacaoCadUnico verificacao,
            boolean precisaDeAnaliseHumana,
            String observacao) { }

    /**
     * RN08 + RN10 - a central confirma à mão um NIS que a consulta automática
     * não encontrou.
     */
    @Transactional
    public VerificacaoCadUnico confirmarNaCentral(Long beneficiarioId, Usuario analista) {
        if (!analista.temPapel(Papel.ADMIN)) {
            throw new RegraDeNegocioViolada("PERMISSAO",
                    "Somente a central pode confirmar uma verificação à mão");
        }
        VerificacaoCadUnico pendente = verificacoes
                .findFirstByUsuarioIdOrderByValidoAteDesc(beneficiarioId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Verificação de NIS", beneficiarioId));

        pendente.confirmarManualmente(propriedades.cadunico().mesesDeValidade());
        avisarConfirmacao(pendente);
        return pendente;
    }

    /** UC04 A2 - tenta de novo as consultas que pegaram o Portal fora do ar. */
    @Transactional
    public int reconsultarPendentes() {
        List<VerificacaoCadUnico> pendentes = verificacoes.findByConsultaPendenteTrue();
        for (VerificacaoCadUnico verificacao : pendentes) {
            var resultado = consultaCadUnico.consultar(verificacao.getNis());
            if (resultado.indisponivel()) {
                continue;   // continua pendente; a proxima rodada tenta de novo
            }
            verificacao.registrarNovaConsulta(resultado.confirmado(), propriedades.cadunico().mesesDeValidade());
            if (resultado.confirmado()) {
                avisarConfirmacao(verificacao);
            }
        }
        return pendentes.size();
    }

    private void avisarConfirmacao(VerificacaoCadUnico verificacao) {
        notificacoes.avisar(verificacao.getUsuario(), TipoNotificacao.CADUNICO_CONFIRMADO,
                "CadÚnico confirmado",
                "Seu NIS foi confirmado. Você já pode fazer pedidos de medicamento no MedShare.");
        ofertarParaPedidosDe(verificacao.getUsuario());
    }

    private void ofertarParaPedidosDe(Usuario beneficiario) {
        necessidades.findByBeneficiarioIdAndAtivaTrue(beneficiario.getId())
                .forEach(ofertas::ofertarParaNecessidade);
    }

    // --- UC04: pedido e receita ----------------------------------------------

    /**
     * UC04 - registra o pedido.
     *
     * RN08: so pede quem tem o CadUnico confirmado e vigente.
     * RN07: so medicamento de alto custo.
     * A4: ja existe pedido ativo do mesmo principio ativo? Atualiza aquele em
     * vez de duplicar — do ponto de vista da pessoa, pedir de novo nao e erro.
     */
    @Transactional
    public Necessidade registrar(Long medicamentoId, Usuario beneficiario) {
        exigirCadUnicoVigente(beneficiario);

        Medicamento medicamento = medicamentos.findById(medicamentoId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Medicamento", medicamentoId));
        exigirMedicamentoDeAltoCusto(medicamento);

        return necessidades
                .ativaDoPrincipioAtivo(beneficiario.getId(), medicamento.getPrincipioAtivo())
                .map(existente -> {
                    existente.trocarMedicamento(medicamento);
                    return existente;
                })
                .orElseGet(() -> necessidades.save(new Necessidade(beneficiario, medicamento)));
    }

    /**
     * RN03 - a receita fica anexada ao pedido e e conferida na retirada.
     * Receita nova tambem tira o pedido de revisao (UC07 A1) e dispara o
     * matching: pode haver caixa esperando por ela.
     */
    @Transactional
    public Receita anexarReceita(Long necessidadeId, String fotoUrl, LocalDate dataEmissao,
                                 LocalDate validade, String crmMedico, String ufCrm,
                                 Usuario beneficiario) {
        Necessidade necessidade = buscarDoBeneficiario(necessidadeId, beneficiario);

        if (validade.isBefore(LocalDate.now())) {
            throw new RegraDeNegocioViolada("RN03",
                    "Esta receita venceu em %s. Peça ao médico uma receita atualizada.".formatted(validade));
        }
        if (validade.isBefore(dataEmissao)) {
            throw new RegraDeNegocioViolada("RN03",
                    "A validade da receita não pode ser anterior à data de emissão");
        }

        Receita receita = necessidade.getReceita();
        if (receita != null) {
            receita.substituir(fotoUrl, dataEmissao, validade, crmMedico, ufCrm);
            necessidade.receitaRenovada();
        } else {
            receita = receitas.save(new Receita(necessidade, fotoUrl, dataEmissao,
                    validade, crmMedico, ufCrm));
            necessidade.anexarReceita(receita);
        }

        ofertas.ofertarParaNecessidade(necessidade);
        return receita;
    }

    @Transactional
    public void encerrar(Long necessidadeId, Usuario beneficiario) {
        buscarDoBeneficiario(necessidadeId, beneficiario).encerrar();
    }

    @Transactional(readOnly = true)
    public List<Necessidade> ativasDe(Usuario beneficiario) {
        return necessidades.findByBeneficiarioIdAndAtivaTrue(beneficiario.getId());
    }

    // --- UC07 A3: procuradores -----------------------------------------------

    @Transactional
    public Procurador cadastrarProcurador(String nome, String cpf, Usuario beneficiario) {
        String apenasDigitos = cpf.replaceAll("\\D", "");
        if (apenasDigitos.equals(beneficiario.getCpf())) {
            throw new RegraDeNegocioViolada("PROCURADOR", "O titular não precisa ser cadastrado como procurador");
        }
        if (procuradores.existsByBeneficiarioIdAndCpf(beneficiario.getId(), apenasDigitos)) {
            throw new RegraDeNegocioViolada("PROCURADOR", "Esta pessoa já está cadastrada como procuradora");
        }
        return procuradores.save(new Procurador(beneficiario, nome.trim(), apenasDigitos));
    }

    @Transactional(readOnly = true)
    public List<Procurador> procuradoresDe(Usuario beneficiario) {
        return procuradores.findByBeneficiarioIdOrderByNome(beneficiario.getId());
    }

    @Transactional
    public void removerProcurador(Long procuradorId, Usuario beneficiario) {
        Procurador procurador = procuradores.findById(procuradorId)
                .filter(p -> p.getBeneficiario().getId().equals(beneficiario.getId()))
                .orElseThrow(() -> new RecursoNaoEncontrado("Procurador", procuradorId));
        procuradores.delete(procurador);
    }

    // --- guardas -------------------------------------------------------------

    private void exigirCadUnicoVigente(Usuario beneficiario) {
        var ultima = verificacoes.findFirstByUsuarioIdOrderByValidoAteDesc(beneficiario.getId());
        if (ultima.map(VerificacaoCadUnico::estaVigente).orElse(false)) {
            return;
        }
        String mensagem = ultima.isEmpty()
                ? "Para pedir medicamentos é preciso ter NIS ativo no CadÚnico. Informe seu NIS no app."
                : ultima.get().isConsultaPendente()
                        ? "Ainda estamos confirmando seu NIS no Portal da Transparência. Você é avisado assim que confirmar."
                        : "Seu NIS ainda não foi confirmado. Nossa equipe está conferindo; "
                                + "se você não estiver no CadÚnico, procure o CRAS do seu bairro.";
        throw new RegraDeNegocioViolada("RN08", mensagem);
    }

    private Necessidade buscarDoBeneficiario(Long necessidadeId, Usuario beneficiario) {
        Necessidade necessidade = necessidades.findById(necessidadeId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Necessidade", necessidadeId));
        if (!necessidade.getBeneficiario().getId().equals(beneficiario.getId())) {
            throw new RegraDeNegocioViolada("RN05", "Este pedido pertence a outra pessoa");
        }
        return necessidade;
    }

    private void exigirMedicamentoDeAltoCusto(Medicamento medicamento) {
        if (!medicamento.atendeAoPisoDePreco(propriedades.doacao().valorMinimoPmc())) {
            throw new RegraDeNegocioViolada("RN07",
                    "%s custa R$ %s e não entra na rede, que atende medicamentos de alto custo."
                            .formatted(medicamento.getNomeComercial(), medicamento.getPmc()));
        }
    }
}
