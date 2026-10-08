import { useEffect, useRef } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { PontoDeColeta } from '../api/tipos'

/**
 * O mapa das farmácias parceiras.
 *
 * Leaflet com telas do OpenStreetMap: sem chave, sem cadastro e sem conta a
 * pagar — o que importa num projeto cuja regra é custo zero. A atribuição ao
 * OpenStreetMap no canto não é enfeite: é condição de uso das telas deles.
 *
 * O mapa é `aria-hidden` e os marcadores não recebem foco de teclado, de
 * propósito. Mapa é informação visual; quem usa leitor de tela não tira nada
 * de uma mancha de alfinetes. **A lista embaixo carrega tudo** — nome,
 * endereço, horário, vaga, como chegar — e é por ela que a tela funciona sem
 * enxergar. O mapa é o atalho de quem enxerga, não a única porta.
 *
 * O ícone é desenhado em HTML (`divIcon`) em vez do alfinete padrão do
 * Leaflet. Dois motivos: o ícone padrão quebra com empacotador, porque ele
 * procura um PNG num caminho que o Vite move; e assim o alfinete usa o verde
 * da marca e muda de cor quando a farmácia está sem vaga.
 */

/** São Paulo, para quando nenhuma farmácia tiver coordenada. */
const CENTRO_PADRAO: [number, number] = [-23.5505, -46.6333]

function alfinete(semVaga: boolean) {
  const cor = semVaga ? 'var(--tinta-fraca, #67766E)' : 'var(--verde, #0A8568)'
  return L.divIcon({
    className: 'alfinete',
    html: `<span style="background:${cor}"></span>`,
    iconSize: [22, 22],
    iconAnchor: [11, 11],
  })
}

export function MapaDeFarmacias({
  pontos,
  aoEscolher,
}: {
  pontos: PontoDeColeta[]
  aoEscolher?: (id: number) => void
}) {
  const caixa = useRef<HTMLDivElement | null>(null)
  const mapa = useRef<L.Map | null>(null)
  const camada = useRef<L.LayerGroup | null>(null)

  // Cria o mapa uma vez. Recriar a cada render deixaria telas penduradas.
  useEffect(() => {
    if (!caixa.current || mapa.current) return

    mapa.current = L.map(caixa.current, {
      zoomControl: true,
      attributionControl: true,
      keyboard: false,
    }).setView(CENTRO_PADRAO, 11)

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '&copy; OpenStreetMap',
    }).addTo(mapa.current)

    camada.current = L.layerGroup().addTo(mapa.current)

    return () => {
      mapa.current?.remove()
      mapa.current = null
      camada.current = null
    }
  }, [])

  // Redesenha os alfinetes sempre que a lista mudar (filtro, ordenação).
  useEffect(() => {
    const m = mapa.current
    const grupo = camada.current
    if (!m || !grupo) return

    grupo.clearLayers()

    const comCoordenada = pontos.filter(
      (p) => typeof p.latitude === 'number' && typeof p.longitude === 'number',
    )
    if (comCoordenada.length === 0) return

    for (const ponto of comCoordenada) {
      const marcador = L.marker([ponto.latitude!, ponto.longitude!], {
        icon: alfinete(ponto.vagasProximosDias === 0),
        keyboard: false,
        title: ponto.nome,
      })
      marcador.bindPopup(
        `<strong>${escapar(ponto.nome)}</strong><br>${escapar(ponto.endereco)}`,
      )
      if (aoEscolher) marcador.on('click', () => aoEscolher(ponto.id))
      marcador.addTo(grupo)
    }

    const limites = L.latLngBounds(
      comCoordenada.map((p) => [p.latitude!, p.longitude!] as [number, number]),
    )
    m.fitBounds(limites, { padding: [36, 36], maxZoom: 15 })
  }, [pontos, aoEscolher])

  return (
    <div className="mapa-farmacias" aria-hidden="true">
      <div ref={caixa} className="tela-do-mapa" />
    </div>
  )
}

/** O nome vem do banco; sem escapar, um apóstrofo ou um < quebraria o popup. */
function escapar(texto: string): string {
  const caixa = document.createElement('div')
  caixa.textContent = texto
  return caixa.innerHTML
}
