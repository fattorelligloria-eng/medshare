/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * Endereço da API em produção (ex.: https://api.medshare.com.br).
   * Em branco, o painel chama "/api" no mesmo domínio — é o que o proxy do
   * Vite faz em desenvolvimento.
   */
  readonly VITE_MEDSHARE_API?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
