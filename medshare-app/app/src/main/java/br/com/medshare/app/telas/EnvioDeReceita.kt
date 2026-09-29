package br.com.medshare.app.telas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.*
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/**
 * RN03 — envio da receita médica.
 *
 * A foto da receita é dado sensível de saúde: fica guardada com acesso
 * restrito, é vista apenas pelo farmacêutico no momento da retirada, e nunca é
 * enviada para nenhum serviço externo de inteligência artificial — só a foto da
 * embalagem passa por leitura automática.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDeEnvioDeReceita(
    repositorio: Repositorio,
    necessidadeId: Long,
    aoConcluir: () -> Unit,
    aoVoltar: () -> Unit,
) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()

    var arquivo by remember { mutableStateOf<File?>(null) }
    var enderecoDaFoto by remember { mutableStateOf<Uri?>(null) }
    var crm by rememberSaveable { mutableStateOf("") }
    var uf by rememberSaveable { mutableStateOf("SP") }
    var emissao by rememberSaveable { mutableStateOf("") }
    var validade by rememberSaveable { mutableStateOf("") }
    var erro by remember { mutableStateOf<Throwable?>(null) }
    var enviando by remember { mutableStateOf(false) }

    val abrirCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { deuCerto ->
        if (!deuCerto) {
            arquivo = null
            enderecoDaFoto = null
        }
    }

    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedida ->
        if (concedida) {
            val novo = CapturaDeFoto.novoArquivo(contexto)
            arquivo = novo
            val endereco = CapturaDeFoto.enderecoPara(contexto, novo)
            enderecoDaFoto = endereco
            abrirCamera.launch(endereco)
        } else {
            erro = Exception("Sem acesso à câmera não dá para enviar a receita.")
        }
    }

    fun paraIso(texto: String): String? = runCatching {
        val partes = texto.split("/")
        LocalDate.of(partes[2].toInt(), partes[1].toInt(), partes[0].toInt()).toString()
    }.getOrNull()

    val emissaoIso = paraIso(emissao)
    val validadeIso = paraIso(validade)
    val podeEnviar = arquivo != null && crm.isNotBlank() && uf.length == 2 &&
        emissaoIso != null && validadeIso != null

    fun enviar() {
        val foto = arquivo ?: return
        val emitida = emissaoIso ?: return
        val vence = validadeIso ?: return
        erro = null
        enviando = true
        escopo.launch {
            repositorio.enviarFoto(foto)
                .onFailure { enviando = false; erro = it }
                .onSuccess { enviada ->
                    repositorio.anexarReceita(
                        necessidadeId, enviada.url, emitida, vence, crm, uf,
                    ).onSuccess { enviando = false; aoConcluir() }
                        .onFailure { enviando = false; erro = it }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enviar receita") },
                navigationIcon = { BotaoDeVoltar(aoVoltar) },
            )
        },
    ) { espaco ->
        Column(
            Modifier
                .padding(espaco)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            AvisoDeErro(erro)
            if (erro != null) Spacer(Modifier.height(16.dp))

            Text(
                "Fotografe a receita inteira, com o carimbo e a assinatura do médico visíveis.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "A receita é vista apenas pelo farmacêutico na hora da retirada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (enderecoDaFoto != null) {
                AsyncImage(
                    model = enderecoDaFoto,
                    contentDescription = "Foto da receita",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(8.dp))
                BotaoSecundario("Tirar outra foto", aoClicar = {
                    pedirPermissao.launch(android.Manifest.permission.CAMERA)
                })
            } else {
                BotaoSecundario("Fotografar a receita", aoClicar = {
                    pedirPermissao.launch(android.Manifest.permission.CAMERA)
                })
            }

            Spacer(Modifier.height(20.dp))
            Row {
                CampoDeTexto(
                    crm, { crm = it.filter(Char::isDigit).take(10) }, "CRM do médico",
                    tipoDeTeclado = KeyboardType.Number, modifier = Modifier.weight(2f),
                )
                Spacer(Modifier.width(10.dp))
                CampoDeTexto(
                    uf, { uf = it.filter(Char::isLetter).take(2).uppercase() }, "UF",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))
            CampoDeTexto(
                emissao, { emissao = it.take(10) }, "Data de emissão",
                dica = "dd/mm/aaaa", tipoDeTeclado = KeyboardType.Number,
            )
            Spacer(Modifier.height(12.dp))
            CampoDeTexto(
                validade, { validade = it.take(10) }, "Válida até",
                dica = "dd/mm/aaaa", tipoDeTeclado = KeyboardType.Number,
                apoio = "Está impresso na receita. Em geral 30 ou 180 dias após a emissão.",
            )

            Spacer(Modifier.height(24.dp))
            BotaoPrincipal("Enviar receita", ::enviar, habilitado = podeEnviar, ocupado = enviando)
            Spacer(Modifier.height(32.dp))
        }
    }
}
