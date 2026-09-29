package br.com.medshare.app.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.medshare.app.dados.PontoDeColeta
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FUSO = ZoneId.of("America/Sao_Paulo")
private val DIA = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.forLanguageTag("pt-BR"))
private val HORA = DateTimeFormatter.ofPattern("HH:mm")

/**
 * UC02 - escolha da farmácia e do horário para entregar a caixa.
 *
 * A lista de farmácias vem ordenada por distância do endereço cadastrado, e os
 * horários são só os que a farmácia atende e ainda têm vaga. A mesma tela
 * serve para o reagendamento único (A2).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TelaDeAgendamento(
    repositorio: Repositorio,
    codigo: String,
    aoConcluir: () -> Unit,
    aoVoltar: () -> Unit,
) {
    val escopo = rememberCoroutineScope()
    var pontos by remember { mutableStateOf<List<PontoDeColeta>>(emptyList()) }
    var escolhido by remember { mutableStateOf<PontoDeColeta?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<Throwable?>(null) }
    var enviando by remember { mutableStateOf(false) }

    var horarios by remember { mutableStateOf<List<OffsetDateTime>>(emptyList()) }
    var carregandoHorarios by remember { mutableStateOf(false) }
    var dia by remember { mutableStateOf<String?>(null) }
    var quando by remember { mutableStateOf<OffsetDateTime?>(null) }

    LaunchedEffect(Unit) {
        repositorio.pontosProximos().onSuccess { pontos = it }.onFailure { erro = it }
        carregando = false
    }

    LaunchedEffect(escolhido) {
        val ponto = escolhido ?: return@LaunchedEffect
        carregandoHorarios = true
        quando = null
        repositorio.horarios(ponto.id)
            .onSuccess { lista ->
                horarios = lista.mapNotNull { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
                dia = horarios.firstOrNull()?.atZoneSameInstant(FUSO)?.format(DIA)
            }
            .onFailure { erro = it }
        carregandoHorarios = false
    }

    val porDia = remember(horarios) { horarios.groupBy { it.atZoneSameInstant(FUSO).format(DIA) } }

    fun enviar() {
        val ponto = escolhido ?: return
        val momento = quando ?: return
        erro = null
        enviando = true
        escopo.launch {
            repositorio.agendar(codigo, ponto.id, momento.toString())
                .onSuccess { enviando = false; aoConcluir() }
                .onFailure { enviando = false; erro = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agendar entrega") },
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

            Text("Farmácia parceira", style = MaterialTheme.typography.titleMedium)
            Text(
                "As mais próximas do seu endereço aparecem primeiro.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            if (carregando) {
                Carregando()
            } else {
                pontos.forEach { ponto ->
                    val selecionado = escolhido?.id == ponto.id
                    Cartao(
                        Modifier
                            .padding(bottom = 10.dp)
                            .clickable { escolhido = ponto },
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
                            RadioButton(selected = selecionado, onClick = { escolhido = ponto })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(ponto.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${ponto.endereco} — ${ponto.bairro}, ${ponto.municipio}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(ponto.horario, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            if (escolhido != null) {
                Spacer(Modifier.height(12.dp))
                Text("Quando você vai levar?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                when {
                    carregandoHorarios -> Carregando()
                    horarios.isEmpty() -> Text(
                        "Esta farmácia não tem horário livre nas próximas duas semanas. Escolha outra.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    else -> {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            porDia.keys.forEach { chave ->
                                FilterChip(
                                    selected = dia == chave,
                                    onClick = { dia = chave; quando = null },
                                    label = { Text(chave) },
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            dia?.let { porDia[it] }.orEmpty().forEach { horario ->
                                FilterChip(
                                    selected = quando == horario,
                                    onClick = { quando = horario },
                                    label = { Text(horario.atZoneSameInstant(FUSO).format(HORA)) },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            BotaoPrincipal(
                "Confirmar agendamento", ::enviar,
                habilitado = escolhido != null && quando != null, ocupado = enviando,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
