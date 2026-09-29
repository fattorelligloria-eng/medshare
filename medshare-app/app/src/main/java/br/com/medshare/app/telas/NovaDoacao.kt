package br.com.medshare.app.telas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.medshare.app.dados.Medicamento
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.*
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Cadastro de uma doação, em três passos: escolher o medicamento, fotografar a
 * caixa e informar lote e validade.
 *
 * As duas regras que barram a doação (RN07, preço mínimo, e RN02, 30 dias de
 * validade) são conferidas no servidor. Aqui a validade também é conferida
 * antes de enviar — não para substituir a regra, mas para a pessoa não
 * fotografar a caixa e preencher tudo só para receber um "não" no final.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDeNovaDoacao(
    repositorio: Repositorio,
    aoConcluir: () -> Unit,
    aoVoltar: () -> Unit,
) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()

    var medicamento by remember { mutableStateOf<Medicamento?>(null) }
    var lote by rememberSaveable { mutableStateOf("") }
    var dia by rememberSaveable { mutableStateOf("") }
    var mes by rememberSaveable { mutableStateOf("") }
    var ano by rememberSaveable { mutableStateOf("") }
    var quantidade by rememberSaveable { mutableStateOf("1") }
    var lacreDeclarado by rememberSaveable { mutableStateOf(false) }

    var arquivoDaFoto by remember { mutableStateOf<File?>(null) }
    var enderecoDaFoto by remember { mutableStateOf<Uri?>(null) }
    var erro by remember { mutableStateOf<Throwable?>(null) }
    var enviando by remember { mutableStateOf(false) }

    val abrirCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { deuCerto -> if (!deuCerto) { arquivoDaFoto = null; enderecoDaFoto = null } }

    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedida ->
        if (concedida) {
            val arquivo = CapturaDeFoto.novoArquivo(contexto)
            arquivoDaFoto = arquivo
            val endereco = CapturaDeFoto.enderecoPara(contexto, arquivo)
            enderecoDaFoto = endereco
            abrirCamera.launch(endereco)
        } else {
            erro = Exception("Sem acesso à câmera não dá para registrar a caixa.")
        }
    }

    val validade: LocalDate? = remember(dia, mes, ano) {
        runCatching {
            LocalDate.of(ano.toInt(), mes.toInt(), dia.toInt())
        }.getOrNull()?.takeIf { ano.length == 4 }
    }
    val diasDeValidade = validade?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    val validadeCurta = diasDeValidade != null && diasDeValidade < 30

    val caixas = quantidade.toIntOrNull()?.takeIf { it in 1..10 }
    val podeEnviar = medicamento != null && lote.isNotBlank() &&
        validade != null && !validadeCurta && arquivoDaFoto != null &&
        caixas != null && lacreDeclarado

    fun enviar() {
        val escolhido = medicamento ?: return
        val arquivo = arquivoDaFoto ?: return
        val vencimento = validade ?: return

        erro = null
        enviando = true
        escopo.launch {
            // A foto vai primeiro: sem URL de foto o cadastro não pode existir.
            repositorio.enviarFoto(arquivo)
                .onFailure { enviando = false; erro = it }
                .onSuccess { foto ->
                    repositorio.cadastrarDoacao(
                        escolhido.id, lote, vencimento.toString(), foto.url,
                        caixas ?: 1, lacreDeclarado,
                    ).onSuccess { enviando = false; aoConcluir() }
                        .onFailure { enviando = false; erro = it }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (medicamento == null) "Qual medicamento?" else "Dados da caixa") },
                navigationIcon = {
                    BotaoDeVoltar { if (medicamento != null) medicamento = null else aoVoltar() }
                },
            )
        },
    ) { espaco ->
        if (medicamento == null) {
            BuscaDeMedicamento(
                repositorio = repositorio,
                aoEscolher = { medicamento = it },
                modifier = Modifier.padding(espaco),
            )
            return@Scaffold
        }

        Column(
            Modifier
                .padding(espaco)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            AvisoDeErro(erro)
            if (erro != null) Spacer(Modifier.height(16.dp))

            Cartao {
                Text(medicamento!!.nomeComercial, style = MaterialTheme.typography.titleMedium)
                Text(
                    medicamento!!.apresentacao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("Foto da caixa", style = MaterialTheme.typography.titleMedium)
            Text(
                "Fotografe a caixa fechada, mostrando o lacre e a aba onde estão " +
                    "impressos o lote e a validade. A leitura automática confere os " +
                    "dois com o que você digitar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            if (enderecoDaFoto != null) {
                AsyncImage(
                    model = enderecoDaFoto,
                    contentDescription = "Foto da caixa",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(8.dp))
                BotaoSecundario("Tirar outra foto", aoClicar = {
                    pedirPermissao.launch(android.Manifest.permission.CAMERA)
                })
            } else {
                BotaoSecundario("Fotografar a caixa", aoClicar = {
                    pedirPermissao.launch(android.Manifest.permission.CAMERA)
                })
            }

            Spacer(Modifier.height(20.dp))
            CampoDeTexto(
                lote, { lote = it.uppercase() }, "Lote",
                apoio = "Está impresso na lateral ou no fundo da caixa",
            )

            Spacer(Modifier.height(16.dp))
            Text("Validade", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                CampoDeTexto(
                    dia, { dia = it.filter(Char::isDigit).take(2) }, "Dia",
                    tipoDeTeclado = KeyboardType.Number, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                CampoDeTexto(
                    mes, { mes = it.filter(Char::isDigit).take(2) }, "Mês",
                    tipoDeTeclado = KeyboardType.Number, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                CampoDeTexto(
                    ano, { ano = it.filter(Char::isDigit).take(4) }, "Ano",
                    tipoDeTeclado = KeyboardType.Number, modifier = Modifier.weight(1.4f),
                )
            }

            if (validadeCurta) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Essa caixa vence em $diasDeValidade dia(s). A rede precisa de pelo " +
                            "menos 30 dias de validade para dar tempo de chegar a quem precisa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            CampoDeTexto(
                quantidade, { quantidade = it.filter(Char::isDigit).take(2) }, "Quantas caixas iguais?",
                apoio = "De 1 a 10, do mesmo lote. Cada caixa ganha um código próprio.",
                tipoDeTeclado = KeyboardType.Number,
            )

            // RN01 - só entra embalagem lacrada; o doador declara antes de enviar.
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = lacreDeclarado, onCheckedChange = { lacreDeclarado = it })
                Text(
                    "Declaro que a embalagem está lacrada de fábrica e nunca foi aberta.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(24.dp))
            BotaoPrincipal("Enviar doação", ::enviar, habilitado = podeEnviar, ocupado = enviando)
            Spacer(Modifier.height(32.dp))
        }
    }
}
