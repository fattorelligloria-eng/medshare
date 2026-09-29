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
import br.com.medshare.app.dados.DoacaoDetalhada
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.*

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
    var detalhe by remember { mutableStateOf<DoacaoDetalhada?>(null) }
    var erro by remember { mutableStateOf<Throwable?>(null) }
    var carregando by remember { mutableStateOf(true) }

    LaunchedEffect(codigo) {
        carregando = true
        repositorio.detalharDoacao(codigo)
            .onSuccess { detalhe = it }
            .onFailure { erro = it }
        carregando = false
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

            // A única ação que o doador precisa tomar no app.
            if (dados.doacao.status == "PRE_VALIDADA") {
                Spacer(Modifier.height(16.dp))
                BotaoPrincipal("Escolher farmácia e horário", aoClicar = { aoAgendar(codigo) })
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
    "ENTREGA" -> "Entrega"
    "RECUSA" -> "Recusa"
    "CANCELAMENTO" -> "Cancelamento"
    "REJEICAO" -> "Rejeição"
    "DESCARTE" -> "Descarte"
    else -> tipo
}
