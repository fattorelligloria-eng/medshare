package br.com.medshare.app.telas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.medshare.app.dados.Rede
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.*
import kotlinx.coroutines.launch

@Composable
fun TelaDeLogin(
    repositorio: Repositorio,
    aoEntrar: () -> Unit,
    aoIrParaCadastro: () -> Unit,
) {
    val escopo = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var erro by remember { mutableStateOf<Throwable?>(null) }
    var enviando by remember { mutableStateOf(false) }

    var mostrarServidor by rememberSaveable { mutableStateOf(false) }
    var servidor by rememberSaveable { mutableStateOf("") }

    // Carrega o endereço já salvo, ou o padrão embutido no APK.
    LaunchedEffect(Unit) {
        repositorio.guarda.servidor.collect { guardado ->
            if (servidor.isBlank()) servidor = guardado ?: Rede.enderecoPadrao()
        }
    }

    fun entrar() {
        erro = null
        enviando = true
        escopo.launch {
            repositorio.guarda.guardarServidor(servidor)
            repositorio.entrar(email, senha)
                .onSuccess { enviando = false; aoEntrar() }
                .onFailure { enviando = false; erro = it }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        MarcaMedShare()
        Spacer(Modifier.height(8.dp))
        Text(
            "Medicamento que sobra de um tratamento\nchegando a quem não pode comprar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        AvisoDeErro(erro)
        if (erro != null) Spacer(Modifier.height(16.dp))

        CampoDeTexto(email, { email = it }, "E-mail", tipoDeTeclado = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        CampoDeTexto(senha, { senha = it }, "Senha", senha = true)
        Spacer(Modifier.height(20.dp))

        BotaoPrincipal(
            texto = "Entrar",
            aoClicar = ::entrar,
            habilitado = email.isNotBlank() && senha.isNotBlank(),
            ocupado = enviando,
        )
        Spacer(Modifier.height(10.dp))
        BotaoSecundario("Criar uma conta", aoIrParaCadastro, habilitado = !enviando)

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // O mesmo APK precisa poder apontar para servidores diferentes — o da
        // apresentação, o de testes, o da máquina de quem desenvolve. Fica
        // recolhido para não confundir quem só quer usar o app.
        TextButton(onClick = { mostrarServidor = !mostrarServidor }) {
            Text(if (mostrarServidor) "Ocultar servidor" else "Configurar servidor")
        }
        if (mostrarServidor) {
            CampoDeTexto(
                servidor,
                { servidor = it },
                "Endereço do servidor",
                dica = "https://meu-servidor.com.br",
                apoio = "Só mude se souber o endereço da API do MedShare.",
                tipoDeTeclado = KeyboardType.Uri,
            )
        }
    }
}

@Composable
fun MarcaMedShare() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "+",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "MedShare",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
