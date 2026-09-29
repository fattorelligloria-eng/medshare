package br.com.medshare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.dados.Sessao
import br.com.medshare.app.telas.*
import br.com.medshare.app.ui.Carregando
import br.com.medshare.app.ui.Destinos
import br.com.medshare.app.ui.TemaMedShare
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repositorio = (application as AplicacaoMedShare).repositorio

        setContent {
            TemaMedShare {
                Surface(Modifier.fillMaxSize()) {
                    AppMedShare(repositorio)
                }
            }
        }
    }
}

/**
 * Onde o app decide o que mostrar.
 *
 * A sessão é lida do aparelho antes de qualquer tela aparecer: quem já entrou
 * uma vez volta direto para a tela inicial, sem piscar o login no meio.
 */
@Composable
private fun AppMedShare(repositorio: Repositorio) {
    val navegacao = rememberNavController()
    val escopo = rememberCoroutineScope()

    var sessao by remember { mutableStateOf<Sessao?>(null) }
    var lendoSessaoGuardada by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repositorio.guarda.sessao.collect { guardada ->
            sessao = guardada
            lendoSessaoGuardada = false
        }
    }

    if (lendoSessaoGuardada) {
        Carregando()
        return
    }

    val sessaoAtiva = sessao
    if (sessaoAtiva == null) {
        // Fora da sessão só existem duas telas, então não vale um NavHost.
        var mostrandoCadastro by remember { mutableStateOf(false) }
        if (mostrandoCadastro) {
            TelaDeCadastro(
                repositorio = repositorio,
                aoCadastrar = { mostrandoCadastro = false },
                aoVoltar = { mostrandoCadastro = false },
            )
        } else {
            TelaDeLogin(
                repositorio = repositorio,
                aoEntrar = { },
                aoIrParaCadastro = { mostrandoCadastro = true },
            )
        }
        return
    }

    NavHost(navegacao, startDestination = Destinos.INICIO) {

        composable(Destinos.INICIO) {
            TelaInicial(
                repositorio = repositorio,
                sessao = sessaoAtiva,
                aoSair = { escopo.launch { repositorio.sair() } },
                aoNovaDoacao = { navegacao.navigate(Destinos.NOVA_DOACAO) },
                aoAbrirDoacao = { navegacao.navigate(Destinos.detalheDaDoacao(it)) },
                aoNovoPedido = { navegacao.navigate(Destinos.NOVO_PEDIDO) },
                aoEnviarReceita = { navegacao.navigate(Destinos.receita(it)) },
                aoVerificarCadUnico = { navegacao.navigate(Destinos.CADUNICO) },
            )
        }

        composable(Destinos.NOVA_DOACAO) {
            TelaDeNovaDoacao(
                repositorio = repositorio,
                aoConcluir = { navegacao.popBackStack() },
                aoVoltar = { navegacao.popBackStack() },
            )
        }

        composable(
            Destinos.DETALHE_DA_DOACAO,
            arguments = listOf(navArgument("codigo") { type = NavType.StringType }),
        ) { entrada ->
            TelaDeDetalheDaDoacao(
                repositorio = repositorio,
                codigo = entrada.arguments?.getString("codigo").orEmpty(),
                aoVoltar = { navegacao.popBackStack() },
                aoAgendar = { navegacao.navigate(Destinos.agendamento(it)) },
            )
        }

        composable(
            Destinos.AGENDAMENTO,
            arguments = listOf(navArgument("codigo") { type = NavType.StringType }),
        ) { entrada ->
            TelaDeAgendamento(
                repositorio = repositorio,
                codigo = entrada.arguments?.getString("codigo").orEmpty(),
                // Volta até a lista: a tela de detalhe atrás está desatualizada.
                aoConcluir = { navegacao.popBackStack(Destinos.INICIO, inclusive = false) },
                aoVoltar = { navegacao.popBackStack() },
            )
        }

        composable(Destinos.CADUNICO) {
            TelaDeCadUnico(
                repositorio = repositorio,
                aoConcluir = { navegacao.popBackStack() },
                aoVoltar = { navegacao.popBackStack() },
            )
        }

        composable(Destinos.NOVO_PEDIDO) {
            TelaDeNovoPedido(
                repositorio = repositorio,
                aoConcluir = { id ->
                    // Já emenda no envio da receita: sem ela o pedido não anda.
                    navegacao.popBackStack()
                    navegacao.navigate(Destinos.receita(id))
                },
                aoVoltar = { navegacao.popBackStack() },
            )
        }

        composable(
            Destinos.RECEITA,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entrada ->
            TelaDeEnvioDeReceita(
                repositorio = repositorio,
                necessidadeId = entrada.arguments?.getLong("id") ?: 0L,
                aoConcluir = { navegacao.popBackStack() },
                aoVoltar = { navegacao.popBackStack() },
            )
        }
    }
}
