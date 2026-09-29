package br.com.medshare.app.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import br.com.medshare.app.dados.DoacaoDetalhada
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * O percurso da caixa, do cadastro até onde ela está agora (RN06).
 *
 * Nenhum evento pode ser alterado nem apagado — nem pela equipe. Quem doou
 * consegue ver exatamente por onde a doação passou e quem a conferiu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDeDetalheDaDoacao(
    repositorio: Repositorio,
    codigo: String,
    aoVoltar: () -> Unit,
    aoAgendar: (String) -> Unit,
) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    var detalhe by remember { mutableStateOf<DoacaoDetalhada?>(null) }
    var erro by remember { mutableStateOf<Throwable?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var versao by remember { mutableIntStateOf(0) }
    var ocupado by remember { mutableStateOf(false) }
    var arquivoDaFoto by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(codigo, versao) {
        carregando = true
        repositorio.detalharDoacao(codigo)
            .onSuccess { detalhe = it }
            .onFailure { erro = it }
        carregando = false
    }

    // UC10 A2 - a central pediu outra foto: tira, envia e a pré-validação roda de novo.
    val abrirCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { deuCerto ->
        val arquivo = arquivoDaFoto
        if (!deuCerto || arquivo == null) return@rememberLauncherForActivityResult
        ocupado = true
        escopo.launch {
            repositorio.trocarFoto(codigo, arquivo)
                .onSuccess { versao++ }
                .onFailure { erro = it }
            ocupado = false
        }
    }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) {
            val novo = CapturaDeFoto.novoArquivo(contexto)
            arquivoDaFoto = novo
            abrirCamera.launch(CapturaDeFoto.enderecoPara(contexto, novo))
        } else {
            erro = Exception("Sem acesso à câmera não dá para enviar a nova foto.")
        }
    }

    fun cancelarAgendamento() {
        ocupado = true
        escopo.launch {
            repositorio.cancelarAgendamento(codigo)
                .onSuccess { versao++ }
                .onFailure { erro = it }
            ocupado = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(codigo, fontFamily = FontFamily.Monospace) },
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
            if (carregando) {
                Carregando()
                return@Column
            }
            val dados = detalhe ?: return@Column

            Cartao {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(dados.doacao.medicamento, style = MaterialTheme.typography.titleLarge)
                    EtiquetaDeStatus(dados.doacao.status)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    Formatos.explicacaoDoStatus(dados.doacao.status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                LinhaDeDado("Princípio ativo", dados.doacao.principioAtivo)
                Spacer(Modifier.height(10.dp))
                LinhaDeDado("Lote", dados.doacao.lote)
                Spacer(Modifier.height(10.dp))
                LinhaDeDado("Validade", Formatos.data(dados.doacao.validade))
                dados.doacao.pontoDeColeta?.let {
                    Spacer(Modifier.height(10.dp))
                    LinhaDeDado("Farmácia", it)
                }
            }

            if (dados.doacao.status == "PRE_VALIDADA") {
                Spacer(Modifier.height(16.dp))
                BotaoPrincipal("Escolher farmácia e horário", aoClicar = { aoAgendar(codigo) })
            }

            // UC03 passo 1 - o código que o doador mostra no balcão.
            val agendamento = dados.agendamento
            if (dados.doacao.status == "AGENDADA" && agendamento != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CÓDIGO DE ENTREGA", style = MaterialTheme.typography.labelSmall)
                        Text(
                            agendamento.codigoEntrega,
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${Formatos.dataComHora(agendamento.dataHora)} — ${agendamento.pontoDeColeta}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(agendamento.endereco, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                BotaoSecundario("Cancelar agendamento", ::cancelarAgendamento, habilitado = !ocupado)
            }

            // UC02 A2 - reagendamento, uma única vez.
            if (dados.podeReagendar) {
                Spacer(Modifier.height(16.dp))
                BotaoPrincipal("Reagendar entrega (uma vez)", aoClicar = { aoAgendar(codigo) })
            }

            // UC10 A2 - a central pediu uma nova foto.
            val ultimo = dados.historico.lastOrNull()
            if (dados.doacao.status == "CADASTRADA" && ultimo?.tipo == "NOVA_FOTO_SOLICITADA") {
                Spacer(Modifier.height(16.dp))
                AvisoDeErro(Exception(ultimo?.descricao ?: "A equipe pediu uma nova foto da embalagem."))
                Spacer(Modifier.height(8.dp))
                BotaoPrincipal(
                    "Tirar nova foto",
                    aoClicar = { pedirPermissao.launch(android.Manifest.permission.CAMERA) },
                    ocupado = ocupado,
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("Histórico", style = MaterialTheme.typography.titleMedium)
            Text(
                "Registro completo e imutável desta caixa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            dados.historico.forEachIndexed { indice, evento ->
                LinhaDoTempo(
                    quando = Formatos.dataComHora(evento.quando),
                    titulo = tituloDoEvento(evento.tipo),
                    descricao = evento.descricao,
                    ehUltimo = indice == dados.historico.lastIndex,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LinhaDoTempo(quando: String, titulo: String, descricao: String, ehUltimo: Boolean) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            if (!ehUltimo) {
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outline),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(bottom = 18.dp)) {
            Text(
                quando,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                descricao,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun tituloDoEvento(tipo: String) = when (tipo) {
    "CADASTRO" -> "Cadastro"
    "PRE_VALIDACAO" -> "Pré-validação"
    "ENVIO_PARA_CENTRAL" -> "Enviada para a central"
    "DECISAO_DA_CENTRAL" -> "Decisão da central"
    "AGENDAMENTO" -> "Agendamento"
    "RECEBIMENTO" -> "Recebimento"
    "VALIDACAO" -> "Conferência do farmacêutico"
    "DISPONIBILIZACAO" -> "Disponibilizada"
    "RESERVA" -> "Reservada"
    "EXPIRACAO_DE_RESERVA" -> "Reserva expirada"
    "CANCELAMENTO_DE_RESERVA" -> "Reserva cancelada"
    "NOVA_FOTO_SOLICITADA" -> "Nova foto solicitada"
    "NOVA_FOTO_ENVIADA" -> "Nova foto enviada"
    "REAGENDAMENTO" -> "Reagendamento"
    "CORRECAO_DE_DADOS" -> "Correção no balcão"
    "TRANSFERENCIA" -> "Transferência entre farmácias"
    "OFERTA" -> "Oferecida a quem precisa"
    "ENTREGA" -> "Entrega"
    "RECUSA" -> "Recusa"
    "CANCELAMENTO" -> "Cancelamento"
    "REJEICAO" -> "Rejeição"
    "DESCARTE" -> "Descarte"
    else -> tipo
}
