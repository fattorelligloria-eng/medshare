package br.com.medshare.app.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.medshare.app.dados.Medicamento
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.*
import kotlinx.coroutines.delay

/**
 * Busca no catálogo, reaproveitada pelo doador e pelo beneficiário.
 *
 * A busca espera 400 ms depois da última tecla antes de chamar a API. Sem isso,
 * digitar "alecensa" dispararia oito requisições, e as respostas poderiam
 * chegar fora de ordem — a tela mostraria o resultado de "alec" depois do
 * resultado completo.
 *
 * O catálogo só devolve medicamentos de alto custo (RN07). Quem procura
 * dipirona aqui não encontra, e isso é a regra funcionando.
 */
@Composable
fun BuscaDeMedicamento(
    repositorio: Repositorio,
    aoEscolher: (Medicamento) -> Unit,
    modifier: Modifier = Modifier,
) {
    var termo by remember { mutableStateOf("") }
    var resultados by remember { mutableStateOf<List<Medicamento>>(emptyList()) }
    var buscando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(termo) {
        if (termo.length < 2) {
            resultados = emptyList()
            return@LaunchedEffect
        }
        delay(400)
        buscando = true
        repositorio.buscarMedicamentos(termo)
            .onSuccess { resultados = it; erro = null }
            .onFailure { erro = it }
        buscando = false
    }

    Column(modifier.fillMaxSize()) {
        CampoDeTexto(
            termo, { termo = it }, "Buscar medicamento",
            dica = "Nome ou princípio ativo",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))

        AvisoDeErro(erro, Modifier.padding(horizontal = 16.dp))

        when {
            buscando -> Carregando()

            termo.length < 2 -> EstadoVazio(
                "Digite para buscar",
                "A rede trabalha só com medicamentos de alto custo, acima de R$ 150.",
            )

            resultados.isEmpty() -> EstadoVazio(
                "Nada encontrado",
                "Confira a grafia. Lembrando que medicamentos abaixo de R$ 150 não entram na rede.",
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(resultados, key = { it.id }) { medicamento ->
                    Cartao(Modifier.clickable { aoEscolher(medicamento) }) {
                        Text(
                            medicamento.nomeComercial,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            medicamento.principioAtivo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(medicamento.apresentacao, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Preço de referência: ${Formatos.reais(medicamento.pmc)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
