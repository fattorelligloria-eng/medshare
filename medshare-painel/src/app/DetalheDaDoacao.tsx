import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/cliente'
import type { DoacaoDetalhada } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'
import { Status, explicar } from '../componentes/Status'
import { data, dataComHora } from '../formatos'

const NOME_DO_EVENTO: Record<string, string> = {
  CADASTRO: 'Cadastro',
  PRE_VALIDACAO: 'Pré-validação',
  ENVIO_PARA_CENTRAL: 'Enviada para a central',
  DECISAO_DA_CENTRAL: 'Decisão da central',
  AGENDAMENTO: 'Agendamento',
  RECEBIMENTO: 'Recebimento',
  VALIDACAO: 'Conferência do farmacêutico',
  DISPONIBILIZACAO: 'Disponibilizada',
  RESERVA: 'Reservada',
  EXPIRACAO_DE_RESERVA: 'Reserva expirada',
  ENTREGA: 'Entrega',
  RECUSA: 'Recusa',
  CANCELAMENTO: 'Cancelamento',
  REJEICAO: 'Rejeição',
  DESCARTE: 'Descarte',
}

/** RN06 — o percurso completo da caixa, do cadastro até onde ela está agora. */
export function DetalheDaDoacao() {
  const { codigo = '' } = useParams()
  const [detalhe, definirDetalhe] = useState<DoacaoDetalhada | null>(null)
  const [erro, definirErro] = useState<unknown>(null)
  const [carregando, definirCarregando] = useState(true)

  useEffect(() => {
    definirCarregando(true)
    api.get<DoacaoDetalhada>(`/doacoes/${codigo}`)
      .then(definirDetalhe)
      .catch(definirErro)
      .finally(() => definirCarregando(false))
  }, [codigo])

  return (
    <>
      <CabecalhoInterno titulo={codigo} para="/app/doacoes" />

      <div className="conteudo-app" style={{ paddingTop: 22, flex: 1 }}>
        <AvisoDeErro erro={erro} />
        {carregando && <p style={{ color: 'var(--tinta-fraca)', fontSize: 14 }}>Carregando…</p>}

        {detalhe && (
          <>
            <Status status={detalhe.doacao.status} />
            <h1 className="humano" style={{ fontSize: 27 }}>{detalhe.doacao.medicamento}</h1>
            <p style={{ margin: '10px 0 0', fontSize: 14.5, lineHeight: 1.5, color: 'var(--tinta-media)' }}>
              {explicar(detalhe.doacao.status)}
            </p>

            <div style={{ margin: '22px 0', paddingTop: 20, borderTop: '1px solid var(--linha)' }}>
              <div className="grade">
                <div className="dado">
                  <p className="rotulo">Princípio ativo</p>
                  <p className="valor">{detalhe.doacao.principioAtivo}</p>
                </div>
                <div className="dado">
                  <p className="rotulo">Lote</p>
                  <p className="valor">{detalhe.doacao.lote}</p>
                </div>
                <div className="dado">
                  <p className="rotulo">Validade</p>
                  <p className="valor">{data(detalhe.doacao.validade)}</p>
                </div>
                {detalhe.doacao.pontoDeColeta && (
                  <div className="dado">
                    <p className="rotulo">Farmácia</p>
                    <p className="valor">{detalhe.doacao.pontoDeColeta}</p>
                  </div>
                )}
              </div>
            </div>

            {detalhe.doacao.status === 'PRE_VALIDADA' && (
              <Link to={`/app/doacoes/${codigo}/agendar`} className="botao principal" style={{ marginBottom: 24 }}>
                Escolher farmácia e horário
              </Link>
            )}

            <p className="rotulo" style={{ marginBottom: 4 }}>Por onde passou</p>
            <p style={{ margin: '0 0 16px', fontSize: 13, color: 'var(--tinta-media)' }}>
              Registro completo e imutável desta caixa.
            </p>

            <ul className="tempo">
              {detalhe.historico.map((evento, i) => (
                <li key={`${evento.quando}-${i}`}>
                  <p className="quando">{dataComHora(evento.quando)}</p>
                  <p className="titulo">{NOME_DO_EVENTO[evento.tipo] ?? evento.tipo}</p>
                  <p className="descricao">{evento.descricao}</p>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
      <div style={{ height: 26, flexShrink: 0 }} />
    </>
  )
}
