package br.com.medshare.app

import android.app.Application
import br.com.medshare.app.dados.Repositorio

/**
 * Cria o repositório uma vez, quando o app abre.
 *
 * As telas chegam nele pelo Application, em vez de cada uma construir o seu.
 * Sem isso, cada tela teria o próprio cliente HTTP e a própria cópia do token.
 */
class AplicacaoMedShare : Application() {

    val repositorio: Repositorio by lazy { Repositorio(this) }
}
