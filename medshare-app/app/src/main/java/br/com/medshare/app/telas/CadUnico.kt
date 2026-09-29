package br.com.medshare.app.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.*
import kotlinx.coroutines.launch

/**
 * RN08 — verificação do NIS no CadÚnico.
 *
 * O critério de quem pode receber não foi inventado pelo projeto: é o cadastro
 * oficial do governo federal para programas sociais. A tela explica isso, porque
 * pedir um número de documento sem dizer para quê é o tipo de coisa que faz a
 * pessoa desistir — ou desconfiar, com razão.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDeCadUnico(
    repositorio: Repositorio,
    aoConcluir: () -> Unit,
    aoVoltar: () -> Unit,
) {
    val escopo = rememberCoroutineScope()
    var nis by rememberSaveable { mutableStateOf("") }
    var erro by remember { mutableStateOf<Throwable?>(null) }
    var sucesso by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    fun enviar() {
        erro = null
        sucesso = null
        enviando = true
        escopo.launch {
            repositorio.verificarCadUnico(nis)
                .onSuccess {
                    enviando = false
                    sucesso = if (it.confirmado) {
                        "Tudo certo. Sua verificação vale até ${Formatos.data(it.validoAte)}."
                    } else {
                        // "Não encontrado" não é recusa: a equipe confere à mão.
                        "Não encontramos esse NIS como beneficiário de programa social. " +
                            "Isso não é um não: seu caso foi para a nossa equipe, que " +
                            "confere à mão e responde em até 48 horas."
                    }
                }
                .onFailure { enviando = false; erro = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CadÚnico") },
                navigationIcon = { BotaoDeVoltar(aoVoltar) },
            )
        },
    ) { espaco ->
        Column(Modifier.padding(espaco).fillMaxSize().padding(20.dp)) {
            Text(
                "Para receber medicamentos pelo MedShare é preciso ter NIS ativo no " +
                    "CadÚnico, o cadastro do governo federal para programas sociais. " +
                    "A verificação vale por 12 meses.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Seu NIS está no Cartão do Cidadão, no aplicativo CadÚnico ou no " +
                    "extrato do Bolsa Família.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            AvisoDeErro(erro)
            sucesso?.let { AvisoDeSucesso(it) }
            if (erro != null || sucesso != null) Spacer(Modifier.height(16.dp))

            CampoDeTexto(
                nis, { nis = it.filter(Char::isDigit).take(11) }, "NIS",
                dica = "11 dígitos", apoio = "Somente números",
                tipoDeTeclado = KeyboardType.Number,
            )
            Spacer(Modifier.height(20.dp))

            if (sucesso == null) {
                BotaoPrincipal(
                    "Verificar", ::enviar,
                    habilitado = nis.length == 11, ocupado = enviando,
                )
            } else {
                BotaoPrincipal("Pronto", aoConcluir)
            }
        }
    }
}
