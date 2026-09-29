package br.com.medshare.app.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Prepara o arquivo onde a câmera vai gravar a foto.
 *
 * O arquivo fica no cache do próprio app e é entregue à câmera por FileProvider,
 * nunca por caminho absoluto: desde o Android 7 entregar um file:// para outro
 * aplicativo derruba o app, e além disso exporia o sistema de arquivos inteiro.
 */
object CapturaDeFoto {

    fun novoArquivo(contexto: Context): File {
        val pasta = File(contexto.cacheDir, "fotos").apply { mkdirs() }
        return File(pasta, "foto-${System.currentTimeMillis()}.jpg")
    }

    fun enderecoPara(contexto: Context, arquivo: File): Uri =
        FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", arquivo)
}
