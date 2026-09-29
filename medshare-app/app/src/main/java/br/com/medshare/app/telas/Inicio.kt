package br.com.medshare.app.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.medshare.app.dados.*
import br.com.medshare.app.ui.*
import kotlinx.coroutines.launch

private enum class Aba(val rotulo: String, val icone: ImageVector) {
    DOACOES("Doações", Icons.Default.Inventory2),
    PEDIDOS("Pedidos", Icons.Default.MedicalServices),
    RESERVAS("Reservas", Icons.Default.ConfirmationNumber),
    CONTA("Conta", Icons.Default.Person),
}

/**
 * A tela principal.
 *
 * As abas aparecem conforme o papel de quem entrou: quem só doa não vê pedidos
 * nem reservas. O servidor barra de qualquer jeito, mas mostrar um botão que
 * sempre vai dar erro é uma forma de mentir para o usuário.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaInicial(
    repositorio: Repositorio,
    sessao: Sessao,
    aoSair: () -> Unit,
    aoNovaDoacao: () -> Unit,
    aoAbrirDoacao: (String) -> Unit,
    aoNovoPedido: () -> Unit,
    aoEnviarReceita: (Long) -> Unit,
    aoVerificarCadUnico: () -> Unit,
) {
    val escopo = rememberCoroutineScope()
    val abas = buildList {
        if (sessao.ehDoador()) add(Aba.DOACOES)
        if (sessao.ehBeneficiario()) {
            add(Aba.PEDIDOS)
            add(Aba.RESERVAS)
        }
        add(Aba.CONTA)
    }
    var abaAtual by remember { mutableStateOf(abas.first()) }

    var doacoes by remember { mutableStateOf<List<Doacao>>(emptyList()) }
    var necessidades by remember { mutableStateOf<List<Necessidade>>(emptyList()) }
    var reservas by remember { mutableStateOf<List<Reserva>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<Throwable?>(null) }

    suspend fun recarregar() {
        carregando = true
        erro = null
        if (sessao.ehDoador()) {
            repositorio.minhasDoacoes().onSuccess { doacoes = it }.onFailure { erro = it }
        }
        if (sessao.ehBeneficiario()) {
            repositorio.minhasNecessidades().onSuccess { necessidades = it }.onFailure { erro = it }
            repositorio.minhasReservas().onSuccess { reservas = it }.onFailure { erro = it }
        }
        carregando = false
    }

    LaunchedEffect(Unit) { recarregar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(abaAtual.rotulo) },
                actions = {
                    IconButton(onClick = { escopo.launch { recarregar() } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                abas.forEach { aba ->
                    NavigationBarItem(
                        selected = aba == abaAtual,
                        onClick = { abaAtual = aba },
                        icon = { Icon(aba.icone, contentDescription = null) },
                        label = { Text(aba.rotulo) },
                    )
                }
            }
        },
        floatingActionButton = {
            when (abaAtual) {
                Aba.DOACOES -> ExtendedFloatingActionButton(
                    onClick = aoNovaDoacao,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Doar") },
                )
                Aba.PEDIDOS -> ExtendedFloatingActionButton(
                    onClick = aoNovoPedido,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Pedir") },
                )
                else -> Unit
            }
        },
    ) { espaco ->
        Column(Modifier.padding(espaco).fillMaxSize()) {
            if (erro != null) {
                AvisoDeErro(erro, Modifier.padding(16.dp))
            }
            when {
                carregando -> Carregando()
                abaAtual == Aba.DOACOES -> ListaDeDoacoes(doacoes, aoAbrirDoacao)
                abaAtual == Aba.PEDIDOS -> ListaDePedidos(
                    necessidades, repositorio, aoEnviarReceita,
                ) { escopo.launch { recarregar() } }
                abaAtual == Aba.RESERVAS -> ListaDeReservas(reservas)
                else -> AbaDaConta(sessao, aoSair, aoVerificarCadUnico)
            }
        }
    }
}

@Composable
private fun ListaDeDoacoes(doacoes: List<Doacao>, aoAbrir: (String) -> Unit) {
    if (doacoes.isEmpty()) {
        EstadoVazio(
            "Você ainda não doou nada",
            "Sobrou caixa lacrada de um tratamento? Toque em Doar e leve até uma farmácia parceira.",
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(doacoes, key = { it.codigo }) { doacao ->
            Cartao(Modifier.clickable { aoAbrir(doacao.codigo) }) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(doacao.medicamento, style = MaterialTheme.typography.titleMedium)
                        Text(
                            doacao.codigo,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    EtiquetaDeStatus(doacao.status)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    Formatos.explicacaoDoStatus(doacao.status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (doacao.pontoDeColeta != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Farmácia: ${doacao.pontoDeColeta}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListaDePedidos(
    necessidades: List<Necessidade>,
    repositorio: Repositorio,
    aoEnviarReceita: (Long) -> Unit,
    aoMudarAlgo: () -> Unit,
) {
    val escopo = rememberCoroutineScope()
    var erroDaReserva by remember { mutableStateOf<Throwable?>(null) }
    var reservando by remember { mutableStateOf<Long?>(null) }

    if (necessidades.isEmpty()) {
        EstadoVazio(
            "Nenhum pedido ativo",
            "Toque em Pedir e escolha o medicamento de que você precisa. Avisamos quando aparecer.",
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (erroDaReserva != null) {
            item { AvisoDeErro(erroDaReserva) }
        }
        items(necessidades, key = { it.id }) { pedido ->
            Cartao {
                Text(pedido.medicamento, style = MaterialTheme.typography.titleMedium)
                Text(
                    pedido.principioAtivo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                if (pedido.temReceitaValida) {
                    Text(
                        "Receita válida até ${Formatos.data(pedido.validadeDaReceita)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    BotaoPrincipal(
                        texto = "Procurar disponível agora",
                        ocupado = reservando == pedido.id,
                        aoClicar = {
                            erroDaReserva = null
                            reservando = pedido.id
                            escopo.launch {
                                repositorio.reservar(pedido.id)
                                    .onSuccess { reservando = null; aoMudarAlgo() }
                                    .onFailure { reservando = null; erroDaReserva = it }
                            }
                        },
                    )
                } else {
                    Text(
                        "Falta enviar a receita médica. Sem ela não é possível reservar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    BotaoSecundario("Enviar receita", { aoEnviarReceita(pedido.id) })
                }
            }
        }
    }
}

@Composable
private fun ListaDeReservas(reservas: List<Reserva>) {
    if (reservas.isEmpty()) {
        EstadoVazio(
            "Nenhuma reserva",
            "Quando um medicamento que você pediu aparecer, a reserva vem para cá com o código de retirada.",
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(reservas, key = { it.codigoRetirada }) { reserva ->
            Cartao {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(reserva.medicamento, style = MaterialTheme.typography.titleMedium)
                    EtiquetaDeStatus(reserva.status)
                }
                Text(
                    reserva.apresentacao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (reserva.status == "ATIVA") {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        ) {
                            Text(
                                "CÓDIGO DE RETIRADA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                reserva.codigoRetirada,
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Retire até ${Formatos.dataComHora(reserva.expiraEm)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Leve um documento com foto e a receita.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (reserva.pontoDeColeta != null) {
                    Spacer(Modifier.height(12.dp))
                    LinhaDeDado("Farmácia", reserva.pontoDeColeta)
                    reserva.enderecoDoPonto?.let {
                        Spacer(Modifier.height(6.dp))
                        LinhaDeDado("Endereço", it)
                    }
                    reserva.horarioDoPonto?.let {
                        Spacer(Modifier.height(6.dp))
                        LinhaDeDado("Horário", it)
                    }
                }
            }
        }
    }
}

@Composable
private fun AbaDaConta(sessao: Sessao, aoSair: () -> Unit, aoVerificarCadUnico: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Cartao {
            Text(sessao.nome, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                sessao.papeis.joinToString(", ") { it.lowercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))

        if (sessao.ehBeneficiario()) {
            Cartao {
                Text("Verificação no CadÚnico", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Para receber medicamentos é preciso ter NIS ativo no CadÚnico. " +
                        "A verificação vale por 12 meses.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                BotaoSecundario("Informar meu NIS", aoVerificarCadUnico)
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.weight(1f))
        BotaoSecundario("Sair da conta", aoSair)
    }
}
