/**
 * Esqueleto de carregamento.
 *
 * Em vez da palavra "Carregando…", blocos cinzas no formato do que vai chegar.
 * A diferença não é enfeite: o esqueleto já mostra quantos itens vêm e como
 * eles se parecem, então a tela não dá um salto quando os dados chegam.
 *
 * `aria-hidden` porque isto não é conteúdo — quem usa leitor de tela recebe o
 * aviso pelo `role="status"` de quem chamou, não pela descrição dos blocos.
 */
export function EsqueletoDeLista({ itens = 3 }: { itens?: number }) {
  return (
    <div aria-hidden="true">
      {Array.from({ length: itens }, (_, i) => (
        <div className="item" key={i}>
          <div className="osso" style={{ width: 92, height: 11, marginBottom: 12 }} />
          <div className="osso" style={{ width: '46%', height: 19, marginBottom: 9 }} />
          <div className="osso" style={{ width: '68%', height: 13, marginBottom: 9 }} />
          <div className="osso" style={{ width: '80%', height: 13 }} />
        </div>
      ))}
    </div>
  )
}

/** Um bloco só, para onde não é lista — o cartão do próximo passo, por exemplo. */
export function EsqueletoDeBloco({ altura = 96 }: { altura?: number }) {
  return <div className="osso" style={{ height: altura, borderRadius: 'var(--raio-g)' }} aria-hidden="true" />
}
