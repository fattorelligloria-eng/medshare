import { Navigate, Route, Routes } from 'react-router-dom'
import { useAutenticacao } from './contexto/Autenticacao'

import { Login } from './paginas/Login'
import { Cadastro } from './paginas/Cadastro'

import { AreaDoApp, TelaInterna } from './componentes/AreaDoApp'
import { Doacoes } from './app/Doacoes'
import { NovaDoacao } from './app/NovaDoacao'
import { DetalheDaDoacao } from './app/DetalheDaDoacao'
import { Agendamento } from './app/Agendamento'
import { CadUnico } from './app/CadUnico'
import { Pedidos } from './app/Pedidos'
import { NovoPedido } from './app/NovoPedido'
import { EnvioDeReceita } from './app/EnvioDeReceita'
import { Reservas } from './app/Reservas'
import { Conta } from './app/Conta'
import { Farmacias as FarmaciasDoApp } from './app/Farmacias'
import { Ajuda } from './app/Ajuda'
import { Sobre } from './app/Sobre'

import { Layout } from './componentes/Layout'
import { FilaDoBalcao } from './paginas/FilaDoBalcao'
import { Retirada } from './paginas/Retirada'
import { FilaDaCentral } from './paginas/FilaDaCentral'
import { Rastreio } from './paginas/Rastreio'
import { Farmacias } from './paginas/Farmacias'
import { Notificacoes } from './paginas/Notificacoes'

/**
 * Duas áreas, propositalmente diferentes:
 *
 *   /app     — o produto: o que o doador e o beneficiário usam.
 *   /painel  — a mesa de trabalho da farmácia e da central.
 *
 * As duas abrem no navegador e usam a tela que têm. O /app também roda dentro
 * do aplicativo Android, com as mesmas telas.
 *
 * Quem entra cai na área do seu papel. Quem acumula papéis escolhe pelo menu.
 */
export function App() {
  const { sessao, temPapel } = useAutenticacao()

  if (!sessao) {
    return (
      <Routes>
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="*" element={<Login />} />
      </Routes>
    )
  }

  const telaInicial = temPapel('DOADOR')
    ? '/app/doacoes'
    : temPapel('BENEFICIARIO')
      ? '/app/pedidos'
      : temPapel('FARMACEUTICO')
        ? '/painel/balcao'
        : '/painel/central'

  return (
    <Routes>
      {/* --- o aplicativo, com navegação de baixo --- */}
      <Route element={<AreaDoApp />}>
        {temPapel('DOADOR') && <Route path="/app/doacoes" element={<Doacoes />} />}
        {temPapel('BENEFICIARIO') && (
          <>
            <Route path="/app/pedidos" element={<Pedidos />} />
            <Route path="/app/reservas" element={<Reservas />} />
          </>
        )}
        <Route path="/app/farmacias" element={<FarmaciasDoApp />} />
        <Route path="/app/conta" element={<Conta />} />
        <Route path="/app/ajuda" element={<Ajuda />} />
        <Route path="/app/sobre" element={<Sobre />} />
      </Route>

      {/* --- telas internas do aplicativo, sem navegação de baixo --- */}
      <Route element={<TelaInterna />}>
        <Route path="/app/notificacoes" element={<Notificacoes />} />
        {temPapel('DOADOR') && (
          <>
            <Route path="/app/doacoes/nova" element={<NovaDoacao />} />
            <Route path="/app/doacoes/:codigo" element={<DetalheDaDoacao />} />
            <Route path="/app/doacoes/:codigo/agendar" element={<Agendamento />} />
          </>
        )}
        {temPapel('BENEFICIARIO') && (
          <>
            <Route path="/app/cadunico" element={<CadUnico />} />
            <Route path="/app/pedidos/novo" element={<NovoPedido />} />
            <Route path="/app/pedidos/:id/receita" element={<EnvioDeReceita />} />
          </>
        )}
      </Route>

      {/* --- a mesa de trabalho --- */}
      <Route element={<Layout />}>
        {temPapel('FARMACEUTICO') && (
          <>
            <Route path="/painel/balcao" element={<FilaDoBalcao />} />
            <Route path="/painel/retirada" element={<Retirada />} />
          </>
        )}
        {temPapel('ADMIN') && (
          <>
            <Route path="/painel/central" element={<FilaDaCentral />} />
            <Route path="/painel/farmacias" element={<Farmacias />} />
          </>
        )}
        <Route path="/painel/notificacoes" element={<Notificacoes noPainel />} />
        {(temPapel('FARMACEUTICO') || temPapel('ADMIN')) && (
          <Route path="/painel/rastreio" element={<Rastreio />} />
        )}
        <Route path="/painel" element={<Navigate to={temPapel('FARMACEUTICO') ? '/painel/balcao' : '/painel/central'} replace />} />
      </Route>

      <Route path="*" element={<Navigate to={telaInicial} replace />} />
    </Routes>
  )
}
