package br.com.medshare.app.telas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.medshare.app.dados.Municipio
import br.com.medshare.app.dados.PedidoDeCadastro
import br.com.medshare.app.dados.Repositorio
import br.com.medshare.app.ui.*
import kotlinx.coroutines.launch

/**
 * Cadastro.
 *
 * Os papéis são caixas de marcar, não uma escolha única: quem doou um
 * medicamento hoje pode precisar de outro amanhã, e o modelo do sistema já
 * permite acumular os dois.
 *
 * A lista de municípios vem da API (RN09) — são só os 39 da Grande São Paulo.
 * Deixar o usuário digitar a cidade livremente daria um cadastro que o servidor
 * recusaria depois, sem ele entender por quê.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDeCadastro(
    repositorio: Repositorio,
    aoCadastrar: () -> Unit,
    aoVoltar: () -> Unit,
) {
    val escopo = rememberCoroutineScope()

    var nome by rememberSaveable { mutableStateOf("") }
    var cpf by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var telefone by rememberSaveable { mutableStateOf("") }
    var cep by rememberSaveable { mutableStateOf("") }
    var logradouro by rememberSaveable { mutableStateOf("") }
    var numero by rememberSaveable { mutableStateOf("") }
    var complemento by rememberSaveable { mutableStateOf("") }
    var bairro by rememberSaveable { mutableStateOf("") }

    var municipios by remember { mutableStateOf<List<Municipio>>(emptyList()) }
    var municipioEscolhido by remember { mutableStateOf<Municipio?>(null) }
    var listaAberta by remember { mutableStateOf(false) }

    var querDoar by rememberSaveable { mutableStateOf(true) }
    var querReceber by rememberSaveable { mutableStateOf(false) }

    var erro by remember { mutableStateOf<Throwable?>(null) }
    var enviando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repositorio.municipios().onSuccess { municipios = it }.onFailure { erro = it }
    }

    // RN09 - com o CEP completo, o endereço vem do ViaCEP e já avisa se está
    // fora da Grande São Paulo, antes de a pessoa preencher o resto.
    var avisoDoCep by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(cep, municipios) {
        avisoDoCep = null
        val digitos = cep.filter(Char::isDigit)
        if (digitos.length != 8) return@LaunchedEffect
        repositorio.enderecoPorCep(digitos)
            .onSuccess { e ->
                if (e.logradouro.isNotBlank()) logradouro = e.logradouro
                if (e.bairro.isNotBlank()) bairro = e.bairro
                val daRegiao = municipios.firstOrNull { it.id == e.municipioId }
                if (e.atendido && daRegiao != null) {
                    municipioEscolhido = daRegiao
                } else {
                    municipioEscolhido = null
                    avisoDoCep = "Este CEP é de ${e.municipio}/${e.uf}. O MedShare atende só os 39 municípios da Grande São Paulo."
                }
            }
            .onFailure { avisoDoCep = "Não encontramos este CEP. Confira os números ou preencha o endereço à mão." }
    }

    val podeEnviar = nome.isNotBlank() && cpf.filter(Char::isDigit).length == 11 &&
        email.contains('@') && senha.length >= 8 && cep.filter(Char::isDigit).length == 8 &&
        logradouro.isNotBlank() && numero.isNotBlank() && bairro.isNotBlank() &&
        municipioEscolhido != null && (querDoar || querReceber)

    fun enviar() {
        val municipio = municipioEscolhido ?: return
        erro = null
        enviando = true
        escopo.launch {
            // A conta nasce so doadora; quem quer receber informa o NIS na aba
            // Conta, e e essa confirmacao que concede o papel.
            val papeis = listOf("DOADOR")
            repositorio.cadastrar(
                PedidoDeCadastro(
                    nome = nome.trim(),
                    cpf = cpf.filter(Char::isDigit),
                    email = email.trim().lowercase(),
                    senha = senha,
                    telefone = telefone.filter(Char::isDigit).ifBlank { null },
                    cep = cep.filter(Char::isDigit),
                    logradouro = logradouro.trim(),
                    numero = numero.trim(),
                    complemento = complemento.trim().ifBlank { null },
                    bairro = bairro.trim(),
                    municipioId = municipio.id,
                    papeis = papeis,
                ),
            ).onSuccess { enviando = false; aoCadastrar() }
                .onFailure { enviando = false; erro = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar conta") },
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

            Text("Seus dados", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            CampoDeTexto(nome, { nome = it }, "Nome completo")
            Spacer(Modifier.height(10.dp))
            CampoDeTexto(
                cpf, { cpf = it.filter(Char::isDigit).take(11) }, "CPF",
                dica = "Somente números", tipoDeTeclado = KeyboardType.Number,
            )
            Spacer(Modifier.height(10.dp))
            CampoDeTexto(email, { email = it }, "E-mail", tipoDeTeclado = KeyboardType.Email)
            Spacer(Modifier.height(10.dp))
            CampoDeTexto(
                senha, { senha = it }, "Senha", senha = true,
                apoio = "Pelo menos 8 caracteres",
            )
            Spacer(Modifier.height(10.dp))
            CampoDeTexto(
                telefone, { telefone = it.filter(Char::isDigit).take(11) }, "Telefone",
                dica = "11999990000", tipoDeTeclado = KeyboardType.Phone,
            )

            Spacer(Modifier.height(24.dp))
            Text("Seu endereço", style = MaterialTheme.typography.titleMedium)
            Text(
                "O MedShare atende os 39 municípios da Grande São Paulo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            CampoDeTexto(
                cep, { cep = it.filter(Char::isDigit).take(8) }, "CEP",
                dica = "Somente números", tipoDeTeclado = KeyboardType.Number,
                apoio = avisoDoCep ?: "O endereço é preenchido a partir do CEP.",
            )
            Spacer(Modifier.height(10.dp))
            CampoDeTexto(logradouro, { logradouro = it }, "Rua ou avenida")
            Spacer(Modifier.height(10.dp))
            Row {
                CampoDeTexto(numero, { numero = it }, "Número", modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                CampoDeTexto(
                    complemento, { complemento = it }, "Complemento",
                    modifier = Modifier.weight(1.4f),
                )
            }
            Spacer(Modifier.height(10.dp))
            CampoDeTexto(bairro, { bairro = it }, "Bairro")
            Spacer(Modifier.height(10.dp))

            ExposedDropdownMenuBox(
                expanded = listaAberta,
                onExpandedChange = { listaAberta = !listaAberta },
            ) {
                OutlinedTextField(
                    value = municipioEscolhido?.nome ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Município") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(listaAberta) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = listaAberta,
                    onDismissRequest = { listaAberta = false },
                ) {
                    municipios.forEach { municipio ->
                        DropdownMenuItem(
                            text = { Text(municipio.nome) },
                            onClick = {
                                municipioEscolhido = municipio
                                listaAberta = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("O que você quer fazer?", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pode marcar os dois.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            EscolhaDePapel(
                marcado = querDoar,
                aoMudar = { querDoar = it },
                titulo = "Doar medicamentos",
                descricao = "Tenho caixas lacradas que sobraram de um tratamento.",
            )
            EscolhaDePapel(
                marcado = querReceber,
                aoMudar = { querReceber = it },
                titulo = "Receber medicamentos",
                descricao = "Preciso de um medicamento e não tenho como comprar.",
            )

            Spacer(Modifier.height(24.dp))
            BotaoPrincipal("Criar conta", ::enviar, habilitado = podeEnviar, ocupado = enviando)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EscolhaDePapel(
    marcado: Boolean,
    aoMudar: (Boolean) -> Unit,
    titulo: String,
    descricao: String,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Checkbox(checked = marcado, onCheckedChange = aoMudar)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.padding(top = 12.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                descricao,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BotaoDeVoltar(aoVoltar: () -> Unit) {
    IconButton(onClick = aoVoltar) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
    }
}
