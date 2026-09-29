package br.com.medshare.app.telas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.AvisoDeErro
import br.com.medshare.app.ui.Carregando
import kotlinx.coroutines.launch

/** O beneficiário escolhe o medicamento de que precisa e entra na fila. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDeNovoPedido(
    repositorio: Repositorio,
    aoConcluir: (Long) -> Unit,
    aoVoltar: () -> Unit,
) {
    val escopo = rememberCoroutineScope()
    var erro by remember { mutableStateOf<Throwable?>(null) }
    var enviando by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Do que você precisa?") },
                navigationIcon = { BotaoDeVoltar(aoVoltar) },
            )
        },
    ) { espaco ->
        Column(Modifier.padding(espaco)) {
            AvisoDeErro(erro, Modifier.padding(16.dp))
            if (enviando) {
                Carregando()
            } else {
                BuscaDeMedicamento(
                    repositorio = repositorio,
                    aoEscolher = { medicamento ->
                        erro = null
                        enviando = true
                        escopo.launch {
                            repositorio.criarNecessidade(medicamento.id)
                                .onSuccess { enviando = false; aoConcluir(it.id) }
                                .onFailure { enviando = false; erro = it }
                        }
                    },
                )
            }
        }
    }
}
