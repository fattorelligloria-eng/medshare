package br.com.medshare.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.medshare.app.dados.ErroDaApi

@Composable
fun CampoDeTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    dica: String? = null,
    apoio: String? = null,
    tipoDeTeclado: KeyboardType = KeyboardType.Text,
    senha: Boolean = false,
    linhas: Int = 1,
    habilitado: Boolean = true,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        placeholder = dica?.let { { Text(it) } },
        supportingText = apoio?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = tipoDeTeclado),
        visualTransformation = if (senha) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = linhas == 1,
        minLines = linhas,
        enabled = habilitado,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun BotaoPrincipal(
    texto: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    ocupado: Boolean = false,
) {
    Button(
        onClick = aoClicar,
        enabled = habilitado && !ocupado,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (ocupado) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(texto, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BotaoSecundario(
    texto: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    OutlinedButton(
        onClick = aoClicar,
        enabled = habilitado,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(texto, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Cartao(modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), content = conteudo)
    }
}

@Composable
fun EtiquetaDeStatus(status: String) {
    val fundo: Color
    val texto: Color
    when {
        Formatos.statusEhProblema(status) -> {
            fundo = MaterialTheme.colorScheme.errorContainer
            texto = MaterialTheme.colorScheme.onErrorContainer
        }
        Formatos.statusEhConclusao(status) -> {
            fundo = MaterialTheme.colorScheme.primary
            texto = MaterialTheme.colorScheme.onPrimary
        }
        else -> {
            fundo = MaterialTheme.colorScheme.primaryContainer
            texto = MaterialTheme.colorScheme.onPrimaryContainer
        }
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(fundo)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            Formatos.status(status),
            color = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Mostra o erro do jeito que a API mandou: a regra violada como etiqueta e a
 * explicação em texto. Quem vê "RN02" junto de "precisa de 30 dias de validade"
 * entende o que aconteceu e o que fazer.
 */
@Composable
fun AvisoDeErro(erro: Throwable?, modifier: Modifier = Modifier) {
    if (erro == null) return
    val regra = (erro as? ErroDaApi)?.regra

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                if (regra != null) {
                    Text(
                        regra,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                Text(
                    erro.message ?: "Algo deu errado.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun AvisoDeSucesso(texto: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            texto,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
fun EstadoVazio(titulo: String, descricao: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            descricao,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun Carregando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
fun LinhaDeDado(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            rotulo.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
