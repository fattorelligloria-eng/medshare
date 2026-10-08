package br.com.medshare.app.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
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
    aoAbrirNotificacoes: () -> Unit,
) {
    val escopo = rememberCoroutineScope()
    var naoLidas by remember { mutableLongStateOf(0L) }
    var ofertas by remember { mutableStateOf<List<Oferta>>(emptyList()) }
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
            repositorio.minhasOfertas()
                .onSuccess { lista -> ofertas = lista.filter { it.status == "PENDENTE" } }
                .onFailure { erro = it }
        }
        repositorio.naoLidas().onSuccess { naoLidas = it }
        carregando = false
    }

    LaunchedEffect(Unit) { recarregar() }

    // Enquanto o push (Firebase) não está configurado, o app confere os avisos
    // de minuto em minuto: oferta tem prazo de 24 h para aceitar (UC06).
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            repositorio.naoLidas().onSuccess { naoLidas = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(abaAtual.rotulo) },
                actions = {
                    IconButton(onClick = aoAbrirNotificacoes) {
                        BadgedBox(badge = {
                            if (naoLidas > 0) Badge { Text(if (naoLidas > 9) "9+" else naoLidas.toString()) }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificações: $naoLidas não lidas")
                        }
                    }
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
                    necessidades, ofertas, repositorio, aoEnviarReceita,
                    aoReservar = { abaAtual = Aba.RESERVAS },
                ) { escopo.launch { recarregar() } }
                abaAtual == Aba.RESERVAS -> ListaDeReservas(reservas, repositorio) { escopo.launch { recarregar() } }
                else -> AbaDaConta(sessao, repositorio, aoSair, aoVerificarCadUnico)
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

/**
 * UC05/UC06 - os pedidos e, no topo, as ofertas: quando uma caixa aparece, o
 * sistema oferece a UMA pessoa da fila, com 24 h para aceitar.
 */
@Composable
private fun ListaDePedidos(
    necessidades: List<Necessidade>,
    ofertas: List<Oferta>,
    repositorio: Repositorio,
    aoEnviarReceita: (Long) -> Unit,
    aoReservar: () -> Unit,
    aoMudarAlgo: () -> Unit,
) {
    val escopo = rememberCoroutineScope()
    var erroDaOferta by remember { mutableStateOf<Throwable?>(null) }
    var respondendo by remember { mutableStateOf<Long?>(null) }

    if (necessidades.isEmpty() && ofertas.isEmpty()) {
        EstadoVazio(
            "Nenhum pedido ativo",
            "Toque em Pedir e escolha o medicamento de que você precisa. Avisamos quando aparecer.",
        )
        return
    }

    fun responder(oferta: Oferta, aceitar: Boolean) {
        erroDaOferta = null
        respondendo = oferta.id
        escopo.launch {
            val resultado = if (aceitar) repositorio.aceitarOferta(oferta.id).map { }
            else repositorio.recusarOferta(oferta.id).map { }
            resultado
                .onSuccess { respondendo = null; aoMudarAlgo(); if (aceitar) aoReservar() }
                .onFailure { respondendo = null; erroDaOferta = it; aoMudarAlgo() }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (erroDaOferta != null) {
            item { AvisoDeErro(erroDaOferta) }
        }
        items(ofertas, key = { "oferta-${it.id}" }) { oferta ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("CHEGOU PARA VOCÊ", style = MaterialTheme.typography.labelSmall)
                    Text(oferta.medicamento, style = MaterialTheme.typography.titleLarge)
                    Text("${oferta.apresentacao} · validade ${Formatos.data(oferta.validade)}",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    LinhaDeDado("Farmácia", "${oferta.pontoDeColeta} — ${oferta.enderecoDoPonto}")
                    Spacer(Modifier.height(6.dp))
                    LinhaDeDado("Horário", oferta.horarioDoPonto)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Aceite até ${Formatos.dataComHora(oferta.expiraEm)}. Depois a caixa vai para a próxima pessoa.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(12.dp))
                    BotaoPrincipal("Aceitar e reservar", { responder(oferta, true) }, ocupado = respondendo == oferta.id)
                    Spacer(Modifier.height(8.dp))
                    BotaoSecundario("Recusar", { responder(oferta, false) }, habilitado = respondendo != oferta.id)
                }
            }
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

                when {
                    pedido.emRevisao -> {
                        Text(
                            "A farmácia não pôde entregar: ${pedido.motivoRevisao}. " +
                                "Envie uma receita atualizada para voltar à fila.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(12.dp))
                        BotaoSecundario("Enviar nova receita", { aoEnviarReceita(pedido.id) })
                    }
                    pedido.temReceitaValida -> Text(
                        "Receita válida até ${Formatos.data(pedido.validadeDaReceita)}. " +
                            if (pedido.prioridade) "Você está na frente da fila." else "Você está na fila; avisamos quando chegar.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    else -> {
                        Text(
                            "Falta enviar a receita médica (ou ela venceu). Sem ela não dá para receber ofertas.",
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
}

@Composable
private fun ListaDeReservas(reservas: List<Reserva>, repositorio: Repositorio, aoMudarAlgo: () -> Unit) {
    val escopo = rememberCoroutineScope()
    var erro by remember { mutableStateOf<Throwable?>(null) }
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
        if (erro != null) {
            item { AvisoDeErro(erro) }
        }
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
                        "Leve um documento com foto e a receita. Se outra pessoa for buscar, " +
                            "ela precisa estar cadastrada como procuradora na aba Conta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    BotaoSecundario("Cancelar reserva", {
                        escopo.launch {
                            repositorio.cancelarReserva(reserva.codigoRetirada)
                                .onSuccess { aoMudarAlgo() }
                                .onFailure { erro = it }
                        }
                    })
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
private fun AbaDaConta(
    sessao: Sessao,
    repositorio: Repositorio,
    aoSair: () -> Unit,
    aoVerificarCadUnico: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(20.dp),
    ) {
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

        // Aparece para todos: e confirmando o NIS que alguem vira beneficiario,
        // entao quem so doa precisa achar esta porta.
        Cartao {
            Text(
                if (sessao.ehBeneficiario()) "Verificação no CadÚnico" else "Precisa de um medicamento?",
                style = MaterialTheme.typography.titleMedium,
            )
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

        if (sessao.ehBeneficiario()) {
            CartaoDeProcuradores(repositorio)
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(16.dp))
        BotaoSecundario("Sair da conta", aoSair)
    }
}

/** UC07 A3 - quem pode retirar no lugar do beneficiário. */
@Composable
private fun CartaoDeProcuradores(repositorio: Repositorio) {
    val escopo = rememberCoroutineScope()
    var lista by remember { mutableStateOf<List<Procurador>>(emptyList()) }
    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<Throwable?>(null) }
    var enviando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repositorio.procuradores().onSuccess { lista = it }.onFailure { erro = it }
    }

    Cartao {
        Text("Quem pode retirar por você", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Se você não puder ir à farmácia, cadastre quem vai. A pessoa leva o próprio " +
                "documento, a receita e o código de retirada.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AvisoDeErro(erro, Modifier.padding(top = 8.dp))
        lista.forEach { p ->
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${p.nome} · CPF final ${p.cpf.takeLast(4)}", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    escopo.launch {
                        repositorio.removerProcurador(p.id)
                            .onSuccess { lista = lista - p }
                            .onFailure { erro = it }
                    }
                }) { Text("Remover") }
            }
        }
        Spacer(Modifier.height(12.dp))
        CampoDeTexto(nome, { nome = it.take(120) }, "Nome completo")
        Spacer(Modifier.height(8.dp))
        CampoDeTexto(
            cpf, { cpf = it.filter(Char::isDigit).take(11) }, "CPF",
            tipoDeTeclado = androidx.compose.ui.text.input.KeyboardType.Number,
        )
        Spacer(Modifier.height(10.dp))
        BotaoSecundario(
            "Adicionar pessoa",
            {
                erro = null
                enviando = true
                escopo.launch {
                    repositorio.cadastrarProcurador(nome, cpf)
                        .onSuccess { lista = (lista + it).sortedBy(Procurador::nome); nome = ""; cpf = "" }
                        .onFailure { erro = it }
                    enviando = false
                }
            },
            habilitado = !enviando && nome.trim().length >= 3 && cpf.length == 11,
        )
    }
}
