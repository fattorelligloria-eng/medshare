package br.com.medshare.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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

    /** Abaixo do limite de 8 MB do servidor, com folga. */
    private const val TAMANHO_MAXIMO_EM_BYTES = 6L * 1024 * 1024

    /** Lado maior da foto reduzida: ainda dá para ler lote e validade. */
    private const val LADO_MAXIMO_EM_PIXELS = 2560

    fun novoArquivo(contexto: Context): File {
        val pasta = File(contexto.cacheDir, "fotos").apply { mkdirs() }
        return File(pasta, "foto-${System.currentTimeMillis()}.jpg")
    }

    fun enderecoPara(contexto: Context, arquivo: File): Uri =
        FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", arquivo)

    /**
     * Câmeras de 48 MP e acima geram fotos que passam do limite do servidor.
     * Nesses casos a foto é regravada menor, no mesmo arquivo. A rotação vem do
     * EXIF e é aplicada nos pixels, porque regravar o JPEG descarta o EXIF — sem
     * isso a caixa chegaria deitada para o farmacêutico.
     */
    fun reduzirSeNecessario(arquivo: File) {
        if (arquivo.length() <= TAMANHO_MAXIMO_EM_BYTES) return

        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(arquivo.path, limites)
        var amostragem = 1
        while (maxOf(limites.outWidth, limites.outHeight) / amostragem > LADO_MAXIMO_EM_PIXELS) {
            amostragem *= 2
        }

        val original = BitmapFactory.decodeFile(
            arquivo.path, BitmapFactory.Options().apply { inSampleSize = amostragem },
        ) ?: return
        val girada = girarConformeExif(arquivo, original)

        arquivo.outputStream().use { saida -> girada.compress(Bitmap.CompressFormat.JPEG, 85, saida) }
        if (girada !== original) original.recycle()
        girada.recycle()
    }

    private fun girarConformeExif(arquivo: File, imagem: Bitmap): Bitmap {
        val graus = when (
            ExifInterface(arquivo.path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return imagem
        }
        val rotacao = Matrix().apply { postRotate(graus) }
        return Bitmap.createBitmap(imagem, 0, 0, imagem.width, imagem.height, rotacao, true)
    }
}
