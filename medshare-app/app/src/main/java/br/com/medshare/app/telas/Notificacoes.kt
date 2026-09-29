package br.com.medshare.app.telas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.medshare.app.dados.Notificacao
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.*

/**
 * Os avisos da pessoa: oferta recebida, lembrete de entrega, reserva,
 * CadÚnico. Abrir a tela marca como lido o que estava novo.
 *
 * Enquanto o push (Firebase) não está configurado, é por aqui — e pelo
 * contador no topo da tela inicial — que a pessoa fica sabendo das coisas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDeNotificacoes(repositorio: Repositorio, aoVoltar: () -> Unit) {
    var itens by remember { mutableStateOf<List<Notificacao>>(emptyList()) }
    var erro by remember { mutableStateOf<Throwable?>(null) }
    var carregando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repositorio.notificacoes()
            .onSuccess { lista ->
                itens = lista
                lista.filterNot { it.lida }.forEach { repositorio.marcarComoLida(it.id) }
            }
            .onFailure { erro = it }
        carregando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificações") },
                navigationIcon = { BotaoDeVoltar(aoVoltar) },
            )
        },
    ) { espaco ->
        Column(Modifier.padding(espaco).fillMaxSize()) {
            AvisoDeErro(erro, Modifier.padding(16.dp))
            when {
                carregando -> Carregando()
                itens.isEmpty() -> EstadoVazio(
                    "Nenhuma notificação",
                    "Ofertas, lembretes e respostas da equipe aparecem aqui.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(itens, key = { it.id }) { n ->
                        Cartao {
                            Text(
                                (if (n.lida) "" else "● ") + n.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (n.lida) FontWeight.Medium else FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(n.corpo, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                Formatos.dataComHora(n.quando),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
