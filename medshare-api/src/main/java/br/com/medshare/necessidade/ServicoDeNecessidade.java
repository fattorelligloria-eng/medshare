package br.com.medshare.necessidade;

import br.com.medshare.comum.*;
import br.com.medshare.integracao.ConsultaDeCadUnico;
import br.com.medshare.medicamento.Medicamento;
import br.com.medshare.medicamento.MedicamentoRepository;
import br.com.medshare.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * O lado de quem precisa: cadastro do NIS, do pedido e da receita.
 */
@Service
public class ServicoDeNecessidade {

    private final NecessidadeRepository necessidades;
    private final ReceitaRepository receitas;
    private final VerificacaoCadUnicoRepository verificacoes;
    private final MedicamentoRepository medicamentos;
    private final ConsultaDeCadUnico consultaCadUnico;
    private final PropriedadesDoMedShare propriedades;

    public ServicoDeNecessidade(NecessidadeRepository necessidades, ReceitaRepository receitas,
                                VerificacaoCadUnicoRepository verificacoes,
                                MedicamentoRepository medicamentos,
                                ConsultaDeCadUnico consultaCadUnico,
                                PropriedadesDoMedShare propriedades) {
        this.necessidades = necessidades;
        this.receitas = receitas;
        this.verificacoes = verificacoes;
        this.medicamentos = medicamentos;
        this.consultaCadUnico = consultaCadUnico;
        this.propriedades = propriedades;
    }

    /**
     * RN08 - verifica o NIS e guarda o resultado por 12 meses.
     *
     * Só o formato inválido é recusado na hora: aí o número está errado e a
     * pessoa pode corrigir. "Não encontrado" não é recusa — a consulta oficial
     * só enxerga quem recebe Bolsa Família, e há quem esteja no CadÚnico sem
     * receber. Esses casos ficam gravados como não confirmados e vão para a
     * conferência humana, o mesmo caminho da foto duvidosa na RN10.
     */
    @Transactional
    public ResultadoDaVerificacao verificarCadUnico(String nis, Usuario beneficiario) {
        ConsultaDeCadUnico.ResultadoDaConsulta resultado = consultaCadUnico.consultar(nis);

        if (!resultado.formatoValido()) {
            throw new RegraDeNegocioViolada("RN08", resultado.observacao());
        }

        VerificacaoCadUnico verificacao = verificacoes.save(new VerificacaoCadUnico(
                beneficiario, nis, resultado.confirmado(),
                propriedades.cadunico().mesesDeValidade(), consultaCadUnico.fonte()));

        return new ResultadoDaVerificacao(
                verificacao, resultado.precisaDeAnaliseHumana(), resultado.observacao());
    }

    /** O que a tela precisa saber depois de uma verificação. */
    public record ResultadoDaVerificacao(
            VerificacaoCadUnico verificacao,
            boolean precisaDeAnaliseHumana,
            String observacao) { }

    /**
     * RN08 + RN10 - a central confirma à mão um NIS que a consulta automática
     * não encontrou. É o que permite atender quem está no CadÚnico sem receber
     * Bolsa Família, sem abrir mão do critério.
     */
    @Transactional
    public VerificacaoCadUnico confirmarNaCentral(Long beneficiarioId, Usuario analista) {
        if (!analista.temPapel(br.com.medshare.usuario.Papel.ADMIN)) {
            throw new RegraDeNegocioViolada("PERMISSAO",
                    "Somente a central pode confirmar uma verificação à mão");
        }
        VerificacaoCadUnico pendente = verificacoes
                .findFirstByUsuarioIdOrderByValidoAteDesc(beneficiarioId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Verificação de NIS", beneficiarioId));

        pendente.confirmarManualmente(propriedades.cadunico().mesesDeValidade());
        return pendente;
    }

    /**
     * RN04 - um pedido ativo por medicamento. Se a pessoa pedir de novo o mesmo
     * remedio, devolvemos o pedido que ja existe em vez de criar um duplicado:
     * do ponto de vista dela, pedir duas vezes a mesma coisa nao e erro.
     */
    @Transactional
    public Necessidade registrar(Long medicamentoId, Usuario beneficiario) {
        Medicamento medicamento = medicamentos.findById(medicamentoId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Medicamento", medicamentoId));

        exigirMedicamentoDeAltoCusto(medicamento);

        return necessidades
                .findByBeneficiarioIdAndMedicamentoIdAndAtivaTrue(beneficiario.getId(), medicamentoId)
                .orElseGet(() -> necessidades.save(new Necessidade(beneficiario, medicamento)));
    }

    /** RN03 - a receita fica anexada ao pedido e e conferida na retirada. */
    @Transactional
    public Receita anexarReceita(Long necessidadeId, String fotoUrl, LocalDate dataEmissao,
                                 LocalDate validade, String crmMedico, String ufCrm,
                                 Usuario beneficiario) {
        Necessidade necessidade = buscarDoBeneficiario(necessidadeId, beneficiario);

        if (validade.isBefore(LocalDate.now())) {
            throw new RegraDeNegocioViolada("RN03",
                    "Esta receita venceu em %s. Envie uma dentro da validade.".formatted(validade));
        }

        Receita receita = receitas.save(new Receita(necessidade, fotoUrl, dataEmissao,
                validade, crmMedico, ufCrm));
        necessidade.anexarReceita(receita);
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
