package br.com.medshare.app.dados

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.armazenamento by preferencesDataStore(name = "medshare")

/**
 * Guarda a sessão e o endereço do servidor no aparelho.
 *
 * O endereço da API é configurável de propósito: o mesmo APK precisa funcionar
 * apontando para o servidor da apresentação, para um servidor de testes ou para
 * a máquina de quem está desenvolvendo, sem recompilar nada.
 */
class GuardaDaSessao(private val contexto: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val chaveDaSessao = stringPreferencesKey("sessao")
    private val chaveDoServidor = stringPreferencesKey("servidor")

    val sessao: Flow<Sessao?> = contexto.armazenamento.data.map { guardado ->
        guardado[chaveDaSessao]?.let {
            runCatching { json.decodeFromString(Sessao.serializer(), it) }.getOrNull()
        }
    }

    val servidor: Flow<String?> = contexto.armazenamento.data.map { it[chaveDoServidor] }

    suspend fun guardarSessao(sessao: Sessao) {
        val texto = json.encodeToString(Sessao.serializer(), sessao)
        contexto.armazenamento.edit { it[chaveDaSessao] = texto }
    }

    suspend fun limparSessao() {
        contexto.armazenamento.edit { it.remove(chaveDaSessao) }
    }

    suspend fun guardarServidor(endereco: String) {
        contexto.armazenamento.edit { it[chaveDoServidor] = normalizar(endereco) }
    }

    /** A Retrofit exige que a URL base termine em barra. */
    private fun normalizar(endereco: String): String {
        val limpo = endereco.trim().removeSuffix("/")
        return "$limpo/"
    }
}
