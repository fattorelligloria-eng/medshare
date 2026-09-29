package br.com.medshare.app.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.medshare.app.dados.PontoDeColeta
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Escolha da farmácia e do horário para entregar a caixa.
 *
 * A lista vem ordenada por distância do endereço cadastrado — quem depende de
 * transporte público não deveria atravessar a cidade para doar.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    var dia by rememberSaveable { mutableStateOf("") }
    var mes by rememberSaveable { mutableStateOf("") }
    var hora by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repositorio.pontosProximos().onSuccess { pontos = it }.onFailure { erro = it }
        carregando = false
    }

    val quando: OffsetDateTime? = remember(dia, mes, hora) {
        runCatching {
            val hoje = LocalDate.now()
            val data = LocalDate.of(hoje.year, mes.toInt(), dia.toInt())
                .let { if (it.isBefore(hoje)) it.plusYears(1) else it }
            data.atTime(hora.toInt(), 0)
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime()
        }.getOrNull()
    }

    val podeEnviar = escolhido != null && quando != null && quando.isAfter(OffsetDateTime.now())

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
                                Text(
                                    ponto.nome,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
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

            Spacer(Modifier.height(12.dp))
            Text("Quando você vai levar?", style = MaterialTheme.typography.titleMedium)
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
                    hora, { hora = it.filter(Char::isDigit).take(2) }, "Hora",
                    dica = "14", tipoDeTeclado = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(24.dp))
            BotaoPrincipal(
                "Confirmar agendamento", ::enviar,
                habilitado = podeEnviar, ocupado = enviando,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
