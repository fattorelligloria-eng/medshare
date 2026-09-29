import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Reserva } from '../api/tipos'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { Status } from '../componentes/Status'
import { Marca } from '../componentes/Logo'
import { Lugar, Sino } from '../componentes/Icones'
import { dataComHora } from '../formatos'

/**
 * As reservas, e o código de retirada.
 *
 * O código é o que a pessoa mostra no balcão, então vem em letras grandes e em
 * fonte monoespaçada: são caracteres que serão lidos em voz alta, às vezes numa
 * tela rachada, às vezes por alguém com pressa.
 *
 * RN05: aqui não há nada sobre quem doou. Nome da farmácia, endereço, horário
 * e código — nada do outro lado.
 */
export function Reservas() {
  const [parametros] = useSearchParams()
  const recemCriada = parametros.get('novo')

  const [reservas, definirReservas] = useState<Reserva[]>([])
  const [erro, definirErro] = useState<unknown>(null)
  const [carregando, definirCarregando] = useState(true)

  useEffect(() => {
    api.get<Reserva[]>('/reservas/minhas')
      .then(definirReservas)
      .catch(definirErro)
      .finally(() => definirCarregando(false))
  }, [])

  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
          <button type="button" className="icone-botao" aria-label="Notificações">
            <Sino cor="var(--tinta-media)" />
          </button>
        </div>
      </div>

      <div className="conteudo-app" style={{ paddingTop: 26, flex: 1 }}>
        <h1 className="humano" style={{ fontSize: 26, marginBottom: 18 }}>Suas reservas</h1>

        <AvisoDeErro erro={erro} />
        {recemCriada && (
          <AvisoDeSucesso>
            Reservado. Leve o código abaixo, um documento com foto e a receita.
          </AvisoDeSucesso>
        )}

        {carregando && <p style={{ color: 'var(--tinta-fraca)', fontSize: 14 }}>Carregando…</p>}

        {!carregando && reservas.length === 0 && (
          <div className="vazio">
            <h3>Nenhuma reserva</h3>
            <p>
              Quando um medicamento que você pediu aparecer na rede, a reserva
              vem para cá com o código de retirada.
            </p>
          </div>
        )}

        {reservas.map((r) => (
          <div key={r.codigoRetirada} style={{ marginBottom: 26 }}>
            <Status status={r.status} />
            <h2 className="humano" style={{ fontSize: 21 }}>{r.medicamento}</h2>
            <p className="detalhe">{r.apresentacao}</p>

            {r.status === 'ATIVA' && (
              <>
                <div className="codigo-grande" style={{ marginTop: 16 }}>
                  <p className="rotulo">Código de retirada</p>
                  <p className="valor">{r.codigoRetirada}</p>
                </div>
                <p style={{ margin: '12px 0 0', fontSize: 14, fontWeight: 600 }}>
                  Retire até {dataComHora(r.expiraEm)}
                </p>
                <p style={{ margin: '4px 0 0', fontSize: 13.5, color: 'var(--tinta-media)' }}>
                  Leve um documento com foto e a receita.
                </p>
              </>
            )}

            {r.pontoDeColeta && (
              <div style={{ marginTop: 14, paddingTop: 14, borderTop: '1px solid var(--linha)' }}>
                <div className="linha-icone" style={{ marginTop: 0 }}>
                  <Lugar cor="var(--verde)" />
                  <strong>{r.pontoDeColeta}</strong>
                </div>
                {r.enderecoDoPonto && <p className="detalhe">{r.enderecoDoPonto}</p>}
                {r.horarioDoPonto && <p className="detalhe">{r.horarioDoPonto}</p>}
              </div>
            )}
          </div>
        ))}
      </div>
    </>
  )
}
